package com.sharehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

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
            http.oauth2Login(Customizer.withDefaults());
        }

        http
            .logout(logout -> logout.logoutUrl("/api/auth/logout"));

        http.addFilterBefore(adminTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AdminTokenFilter adminTokenFilter(Environment environment) {
        String token = environment.getProperty("sharehub.admin.token", AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
        return new AdminTokenFilter(token);
    }
}
