package com.sharehub.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

class OAuthLoginSuccessHandlerTest {

    @Test
    void shouldRedirectToFrontendAuthCallback() throws Exception {
        OAuthLoginSuccessHandler handler = new OAuthLoginSuccessHandler("http://127.0.0.1:14173");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/github");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(18080);
        request.setRequestURI("/login/oauth2/code/github");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
            request,
            response,
            new TestingAuthenticationToken("github-user", "n/a")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://127.0.0.1:14173/auth/callback");
    }

    @Test
    void shouldCarryCapturedRedirectToFrontendAuthCallback() throws Exception {
        OAuthLoginSuccessHandler handler = new OAuthLoginSuccessHandler("http://127.0.0.1:14173");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/github");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(18080);
        request.getSession(true).setAttribute(OAuthRedirectCaptureFilter.SESSION_KEY, "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
            request,
            response,
            new TestingAuthenticationToken("github-user", "n/a")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://127.0.0.1:14173/auth/callback?redirect=/admin");
    }
}
