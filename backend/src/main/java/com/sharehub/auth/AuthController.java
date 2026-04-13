package com.sharehub.auth;

import com.sharehub.common.ApiResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.sharehub.config.AdminTokenFilter;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final FileStorageService fileStorageService;
    private final UserProfileRepository userProfileRepository;
    private final RequestAccessService requestAccessService;
    private final String expectedAdminToken;

    public AuthController(
        FileStorageService fileStorageService,
        UserProfileRepository userProfileRepository,
        RequestAccessService requestAccessService,
        Environment environment
    ) {
        this.fileStorageService = fileStorageService;
        this.userProfileRepository = userProfileRepository;
        this.requestAccessService = requestAccessService;
        this.expectedAdminToken = environment.getProperty("sharehub.admin.token", AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
    }

    @GetMapping("/github/login")
    public ApiResponse<Map<String, String>> githubLogin() {
        Map<String, String> result = new HashMap<>();
        result.put("loginUrl", "/oauth2/authorization/github");
        return ApiResponse.ok(result);
    }

    @GetMapping("/github/callback")
    public ApiResponse<Map<String, String>> callback() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "OAuth callback handled by Spring Security (/login/oauth2/code/github)");
        return ApiResponse.ok(result);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me(Authentication authentication, HttpServletRequest request) {
        String login = requestAccessService.requireUser(authentication, request);
        String name = resolveName(authentication, login);
        UserProfileDto profile = userProfileRepository.upsert(login, name, null);
        userProfileRepository.ensureActive(login);
        UserProfileDto response = new UserProfileDto(
            profile.id(),
            profile.login(),
            profile.name(),
            profile.bio(),
            profile.avatarFileId(),
            profile.avatarUrl(),
            profile.status(),
            hasAdminRole(authentication, request)
        );
        return ApiResponse.ok(response);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.ok("LOGOUT_SUCCESS");
    }

    @PostMapping(path = "/avatar", consumes = "multipart/form-data")
    public ApiResponse<StoredFileDto> uploadAvatar(
        Authentication authentication,
        HttpServletRequest request,
        @RequestPart("file") MultipartFile file
    ) {
        String owner = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(owner, resolveName(authentication, owner), null);
        userProfileRepository.ensureActive(owner);
        StoredFileDto stored = fileStorageService.storeMultipart(owner, FileCategory.AVATAR, "USER_AVATAR", owner, file);
        userProfileRepository.updateAvatar(owner, stored.id());
        return ApiResponse.ok(stored);
    }

    private String resolveName(Authentication authentication, String fallback) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User user) {
            Object name = user.getAttribute("name");
            if (name != null && !String.valueOf(name).isBlank()) {
                return String.valueOf(name);
            }
        }
        return fallback;
    }

    private boolean hasAdminRole(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals)) {
            return true;
        }
        if (request == null || expectedAdminToken == null || expectedAdminToken.isBlank()) {
            return false;
        }
        String headerValue = request.getHeader(AdminTokenFilter.HEADER);
        return headerValue != null && expectedAdminToken.equals(headerValue);
    }

}
