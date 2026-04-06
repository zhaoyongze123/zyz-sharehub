package com.sharehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) throws Exception {
        boolean oauthEnabled = environment.getProperty("sharehub.auth.github.enabled", Boolean.class, false);

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/auth/**", "/api/resources/**", "/api/roadmaps/**", "/api/notes/**").permitAll()
                .anyRequest().authenticated());

        if (oauthEnabled) {
            http.oauth2Login(Customizer.withDefaults());
        }

        http
            .logout(logout -> logout.logoutUrl("/api/auth/logout"));

        return http.build();
    }
}
