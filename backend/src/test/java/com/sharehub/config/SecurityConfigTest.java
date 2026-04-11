package com.sharehub.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigTest {

    @Test
    void shouldDisableAdminDevTokenInProductionEvenWhenConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("sharehub.app-env", "production")
            .withProperty("sharehub.admin.dev-token-enabled", "true");

        assertThat(SecurityConfig.isAdminDevTokenEnabled(environment)).isFalse();
    }

    @Test
    void shouldKeepAdminDevTokenEnabledOutsideProductionWhenConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("sharehub.app-env", "cloud-dev")
            .withProperty("sharehub.admin.dev-token-enabled", "true");

        assertThat(SecurityConfig.isAdminDevTokenEnabled(environment)).isTrue();
    }

    @Test
    void shouldKeepAdminDevTokenEnabledInTestWhenConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("sharehub.app-env", "test")
            .withProperty("sharehub.admin.dev-token-enabled", "true");

        assertThat(SecurityConfig.isAdminDevTokenEnabled(environment)).isTrue();
    }

    @Test
    void shouldDisableAdminDevTokenInStagingEvenWhenConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("sharehub.app-env", "staging")
            .withProperty("sharehub.admin.dev-token-enabled", "true");

        assertThat(SecurityConfig.isAdminDevTokenEnabled(environment)).isFalse();
    }

    @Test
    void shouldKeepAdminDevTokenDisabledWhenNotConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("sharehub.app-env", "local");

        assertThat(SecurityConfig.isAdminDevTokenEnabled(environment)).isFalse();
    }
}
