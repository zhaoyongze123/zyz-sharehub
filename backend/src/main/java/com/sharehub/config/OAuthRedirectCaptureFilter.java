package com.sharehub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OAuthRedirectCaptureFilter extends OncePerRequestFilter {

    public static final String SESSION_KEY = "sharehub.oauth.redirect";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (isGithubAuthorizationRequest(request)) {
            String redirect = request.getParameter("redirect");
            if (redirect != null && !redirect.isBlank() && redirect.startsWith("/")) {
                request.getSession(true).setAttribute(SESSION_KEY, redirect);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isGithubAuthorizationRequest(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
            && "/oauth2/authorization/github".equals(request.getRequestURI());
    }
}
