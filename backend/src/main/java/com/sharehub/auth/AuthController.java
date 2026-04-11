package com.sharehub.auth;

import com.sharehub.common.ApiResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final FileStorageService fileStorageService;
    private final UserProfileRepository userProfileRepository;
    private final RequestAccessService requestAccessService;
    private final boolean devUserHeaderEnabled;

    public AuthController(
        FileStorageService fileStorageService,
        UserProfileRepository userProfileRepository,
        RequestAccessService requestAccessService,
        @Value("${sharehub.auth.dev-user-header-enabled:false}") boolean devUserHeaderEnabled
    ) {
        this.fileStorageService = fileStorageService;
        this.userProfileRepository = userProfileRepository;
        this.requestAccessService = requestAccessService;
        this.devUserHeaderEnabled = devUserHeaderEnabled;
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
        String login = requireAuthUser(authentication, request);
        String name = resolveName(authentication, login);
        userProfileRepository.upsert(login, name, null);
        userProfileRepository.ensureActive(login);
        UserProfileDto profile = userProfileRepository.findByLogin(login);
        return ApiResponse.ok(withAdminSessionState(profile, authentication));
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
        String owner = requireAuthUser(authentication, request);
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

    private String requireAuthUser(Authentication authentication, HttpServletRequest request) {
        boolean usesDevUserHeader = request != null && request.getHeader(RequestAccessService.USER_KEY_HEADER) != null;
        boolean oauthAuthenticated = authentication != null && authentication.getPrincipal() instanceof OAuth2User;
        if (usesDevUserHeader && !devUserHeaderEnabled && !oauthAuthenticated) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NOT_LOGGED_IN");
        }
        return requestAccessService.requireUser(authentication, request);
    }

    private UserProfileDto withAdminSessionState(UserProfileDto profile, Authentication authentication) {
        return new UserProfileDto(
            profile.id(),
            profile.login(),
            profile.name(),
            profile.avatarFileId(),
            profile.avatarUrl(),
            profile.status(),
            isAdminSession(authentication, profile.login())
        );
    }

    private boolean isAdminSession(Authentication authentication, String login) {
        if (authentication == null) {
            return false;
        }

        boolean alreadyElevated = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_SUPER_ADMIN"::equals);
        if (alreadyElevated) {
            return true;
        }

        if (authentication.getPrincipal() instanceof OAuth2User) {
            return userProfileRepository.isActiveAdmin(login);
        }

        return false;
    }

}
