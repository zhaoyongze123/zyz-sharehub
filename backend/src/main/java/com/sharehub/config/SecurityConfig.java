package com.sharehub.config;

import com.sharehub.auth.GitHubOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
    private final OAuthRedirectCaptureFilter oAuthRedirectCaptureFilter;
    private final GitHubOAuth2UserService gitHubOAuth2UserService;

    public SecurityConfig(
        OAuthLoginSuccessHandler oAuthLoginSuccessHandler,
        OAuthRedirectCaptureFilter oAuthRedirectCaptureFilter,
        GitHubOAuth2UserService gitHubOAuth2UserService
    ) {
        this.oAuthLoginSuccessHandler = oAuthLoginSuccessHandler;
        this.oAuthRedirectCaptureFilter = oAuthRedirectCaptureFilter;
        this.gitHubOAuth2UserService = gitHubOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) throws Exception {
        AdminTokenFilter adminTokenFilter = adminTokenFilter(environment);
        boolean oauthEnabled = environment.getProperty("sharehub.auth.github.enabled", Boolean.class, false);

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(
                    "/actuator/health",
                    "/api/auth/**",
                    "/api/comments/**",
                    "/api/files/**",
                    "/api/me/**",
                    "/api/reports",
                    "/api/resources/**",
                    "/api/roadmaps/**",
                    "/api/notes/**",
                    "/api/resumes/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated());

        if (oauthEnabled) {
            http.oauth2Login(oauth -> oauth
                .successHandler(oAuthLoginSuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(gitHubOAuth2UserService))
            );
        }

        http
            .logout(logout -> logout.logoutUrl("/api/auth/logout"));

        http.addFilterBefore(oAuthRedirectCaptureFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(adminTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AdminTokenFilter adminTokenFilter(Environment environment) {
        String token = environment.getProperty("sharehub.admin.token", AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
        return new AdminTokenFilter(token);
    }
}
