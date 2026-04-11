package com.sharehub.config;

import com.sharehub.auth.RequestAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "sharehub.admin.dev-token-enabled=true",
        "sharehub.admin.token=dev-admin-token"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTokenFilterDevModeIntegrationTest {

    private static final String ADMIN_LOGIN = "dev-oauth-admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAllowAdminEndpointsViaDevTokenWhenExplicitlyEnabled() throws Exception {
        mockMvc.perform(get("/api/admin/reports").header(AdminTokenFilter.HEADER, "dev-admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldRejectInvalidAdminTokenWhenDevTokenModeEnabled() throws Exception {
        mockMvc.perform(get("/api/admin/reports").header(AdminTokenFilter.HEADER, "wrong-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_INVALID));
    }

    @Test
    void shouldRejectWhitelistedAdminSpoofedViaUserHeaderWhenDevTokenModeEnabled() throws Exception {
        grantAdmin(ADMIN_LOGIN);

        mockMvc.perform(get("/api/admin/reports").header(RequestAccessService.USER_KEY_HEADER, ADMIN_LOGIN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void shouldAllowWhitelistedOauthAdminWhenDevTokenModeEnabled() throws Exception {
        grantAdmin(ADMIN_LOGIN);

        mockMvc.perform(
                get("/api/admin/reports")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", ADMIN_LOGIN);
                        attributes.put("name", "Dev OAuth Admin");
                    }))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    private void grantAdmin(String login) {
        jdbcTemplate.update("DELETE FROM admin_accounts WHERE user_login = ?", login);
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, remark, created_at, updated_at
                ) VALUES (?, 'ACTIVE', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            login,
            "dev-mode-test",
            "dev mode admin"
        );
    }
}
