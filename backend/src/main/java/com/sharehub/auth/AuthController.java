package com.sharehub.auth;

import com.sharehub.common.ApiResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final FileStorageService fileStorageService;

    public AuthController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
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
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User user)) {
            return ApiResponse.fail("NOT_LOGGED_IN");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", user.getAttribute("name"));
        result.put("login", user.getAttribute("login"));
        result.put("avatarUrl", user.getAttribute("avatar_url"));
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.ok("LOGOUT_SUCCESS");
    }

    @PostMapping(path = "/avatar", consumes = "multipart/form-data")
    public ApiResponse<StoredFileDto> uploadAvatar(Authentication authentication, @RequestPart("file") MultipartFile file) {
        String owner = resolveOwner(authentication);
        StoredFileDto stored = fileStorageService.storeMultipart(owner, FileCategory.AVATAR, "USER_AVATAR", owner, file);
        return ApiResponse.ok(stored);
    }

    private String resolveOwner(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User user) {
            Object login = user.getAttribute("login");
            if (login != null) {
                return String.valueOf(login);
            }
        }
        return "local-dev-user";
    }
}
