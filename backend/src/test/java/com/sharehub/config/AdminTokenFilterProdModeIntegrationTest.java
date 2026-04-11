package com.sharehub.config;

import com.sharehub.auth.RequestAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

@SpringBootTest(
    properties = {
        "sharehub.admin.dev-token-enabled=false",
        "sharehub.admin.token=dev-admin-token"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTokenFilterProdModeIntegrationTest {

    private static final String ADMIN_LOGIN = "prod-oauth-admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM admin_accounts WHERE user_login = ?", ADMIN_LOGIN);
    }

    @Test
    void shouldRejectAdminTokenWhenDevTokenModeDisabled() throws Exception {
        mockMvc.perform(get("/api/admin/reports").header(AdminTokenFilter.HEADER, "dev-admin-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void shouldRejectWhitelistedAdminSpoofedViaUserHeaderWhenDevTokenModeDisabled() throws Exception {
        grantAdmin(ADMIN_LOGIN);

        mockMvc.perform(get("/api/admin/reports").header(RequestAccessService.USER_KEY_HEADER, ADMIN_LOGIN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void shouldRejectAdminRootSpoofedViaUserHeaderWhenDevTokenModeDisabled() throws Exception {
        grantAdmin(ADMIN_LOGIN);

        mockMvc.perform(get("/api/admin").header(RequestAccessService.USER_KEY_HEADER, ADMIN_LOGIN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void shouldAllowWhitelistedOauthAdminWhenDevTokenModeDisabled() throws Exception {
        grantAdmin(ADMIN_LOGIN);

        mockMvc.perform(
                get("/api/admin/reports")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", ADMIN_LOGIN);
                        attributes.put("name", "Prod OAuth Admin");
                    }))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldRejectNonWhitelistedOauthUserWhenDevTokenModeDisabled() throws Exception {
        mockMvc.perform(
                get("/api/admin/reports")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", "plain-oauth-user");
                        attributes.put("name", "Plain OAuth User");
                    }))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void shouldRejectRevokedOauthAdminWhenDevTokenModeDisabled() throws Exception {
        revokeAdmin(ADMIN_LOGIN);

        mockMvc.perform(
                get("/api/admin/reports")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", ADMIN_LOGIN);
                        attributes.put("name", "Revoked OAuth Admin");
                    }))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    private void grantAdmin(String login) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, remark, created_at, updated_at
                ) VALUES (?, 'ACTIVE', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            login,
            "prod-mode-test",
            "prod mode admin"
        );
    }

    private void revokeAdmin(String login) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, revoked_by, revoked_at, remark, created_at, updated_at
                ) VALUES (?, 'REVOKED', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (user_login) DO UPDATE SET
                    status = EXCLUDED.status,
                    granted_by = EXCLUDED.granted_by,
                    granted_at = EXCLUDED.granted_at,
                    revoked_by = EXCLUDED.revoked_by,
                    revoked_at = EXCLUDED.revoked_at,
                    remark = EXCLUDED.remark,
                    updated_at = CURRENT_TIMESTAMP
                """,
            login,
            "prod-mode-test",
            "security-audit",
            "revoked prod mode admin"
        );
    }
}
