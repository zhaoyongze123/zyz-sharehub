package com.sharehub.config;

import com.sharehub.admin.AdminAccountRepository;
import com.sharehub.auth.RequestAccessService;
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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Token";
    public static final String DEFAULT_ADMIN_TOKEN = "dev-admin-token";
    public static final String ADMIN_TOKEN_REQUIRED = "ADMIN_TOKEN_REQUIRED";
    public static final String ADMIN_TOKEN_INVALID = "INVALID_ADMIN_TOKEN";

    private final String expectedToken;
    private final boolean devTokenEnabled;
    private final AdminAccountRepository adminAccountRepository;
    private final RequestAccessService requestAccessService;

    public AdminTokenFilter(
        String expectedToken,
        boolean devTokenEnabled,
        AdminAccountRepository adminAccountRepository,
        RequestAccessService requestAccessService
    ) {
        this.expectedToken = expectedToken;
        this.devTokenEnabled = devTokenEnabled;
        this.adminAccountRepository = adminAccountRepository;
        this.requestAccessService = requestAccessService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!isAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isWhitelistedAdmin(request)) {
            Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
            String principal = resolvePrincipal(currentAuthentication, request);
            authenticateAndContinue(principal, filterChain, request, response);
            return;
        }

        if (!devTokenEnabled) {
            reject(response, ADMIN_TOKEN_REQUIRED);
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

        authenticateAndContinue("dev-admin-token", filterChain, request, response);
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && ("/api/admin".equals(path) || path.startsWith("/api/admin/"));
    }

    private boolean isWhitelistedAdmin(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = resolvePrincipal(authentication, request);
        return login != null && adminAccountRepository.isActiveAdmin(login);
    }

    private String resolvePrincipal(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User user) {
            Object login = user.getAttribute("login");
            if (login != null && !String.valueOf(login).isBlank()) {
                return String.valueOf(login);
            }
        }
        if (devTokenEnabled) {
            return requestAccessService.resolveUser(authentication, request).orElse(null);
        }
        return null;
    }

    private void authenticateAndContinue(
        String principal,
        FilterChain filterChain,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException, ServletException {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
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
