package com.sharehub.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RequestAccessService {

    public static final String USER_KEY_HEADER = "X-User-Key";

    private final boolean requireLogin;

    public RequestAccessService(@Value("${sharehub.auth.require-login:true}") boolean requireLogin) {
        this.requireLogin = requireLogin;
    }

    public Optional<String> resolveUser(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User user) {
            Object login = user.getAttribute("login");
            if (login != null && !String.valueOf(login).isBlank()) {
                return Optional.of(String.valueOf(login));
            }
        }

        if (request != null) {
            String userKey = request.getHeader(USER_KEY_HEADER);
            if (userKey != null && !userKey.isBlank()) {
                return Optional.of(userKey.trim());
            }
        }

        if (!requireLogin) {
            return Optional.of("local-dev-user");
        }

        return Optional.empty();
    }

    public String requireUser(Authentication authentication, HttpServletRequest request) {
        return resolveUser(authentication, request)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NOT_LOGGED_IN"));
    }
}
