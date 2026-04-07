package com.sharehub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Token";
    public static final String DEFAULT_ADMIN_TOKEN = "dev-admin-token";
    public static final String ADMIN_TOKEN_REQUIRED = "ADMIN_TOKEN_REQUIRED";
    public static final String ADMIN_TOKEN_INVALID = "INVALID_ADMIN_TOKEN";

    private final String expectedToken;

    public AdminTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!isAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedToken == null || expectedToken.isBlank()) {
            reject(response, ADMIN_TOKEN_REQUIRED);
            return;
        }

        String headerValue = request.getHeader(HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            reject(response, ADMIN_TOKEN_REQUIRED);
            return;
        }

        if (!expectedToken.equals(headerValue)) {
            reject(response, ADMIN_TOKEN_INVALID);
            return;
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "admin",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/admin/");
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "{\"success\":false,\"code\":\"" + message + "\",\"data\":null,\"message\":\"" + message + "\"}"
        );
    }
}
