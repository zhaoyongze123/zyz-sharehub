package com.sharehub.auth;

import com.sharehub.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
}
