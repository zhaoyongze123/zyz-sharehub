package com.sharehub.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SimpleUrlAuthenticationSuccessHandler delegate = new SimpleUrlAuthenticationSuccessHandler();
    private final String frontendBaseUrl;

    public OAuthLoginSuccessHandler(
        @Value("${sharehub.frontend.base-url:}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        delegate.setAlwaysUseDefaultTargetUrl(true);
        delegate.setDefaultTargetUrl(resolveCallbackUrl(request));
        delegate.onAuthenticationSuccess(request, response, authentication);
    }

    private String resolveCallbackUrl(HttpServletRequest request) {
        String baseUrl = frontendBaseUrl;
        if (baseUrl.isBlank()) {
            baseUrl = UriComponentsBuilder
                .fromHttpUrl(request.getRequestURL().toString())
                .replacePath("")
                .replaceQuery(null)
                .build()
                .toUriString();
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/auth/callback");
        Object redirect = request.getSession(false) == null
            ? null
            : request.getSession(false).getAttribute(OAuthRedirectCaptureFilter.SESSION_KEY);
        if (redirect instanceof String redirectPath && !redirectPath.isBlank()) {
            builder.queryParam("redirect", redirectPath);
            request.getSession(false).removeAttribute(OAuthRedirectCaptureFilter.SESSION_KEY);
        }
        return builder.build().toUriString();
    }
}
