package com.sharehub.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdminAccountRepositoryIntegrationTest {

    private static final String LOGIN = "reactivate-admin";

    @Autowired
    private AdminAccountRepository adminAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM admin_accounts WHERE user_login = ?", LOGIN);
    }

    @Test
    void grantAdminShouldReactivateExistingInactiveAccount() {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, revoked_by, revoked_at, remark, created_at, updated_at
                ) VALUES (?, 'REVOKED', ?, ?, ?, ?, ?, ?, ?)
                """,
            LOGIN,
            "initial-admin",
            Timestamp.from(Instant.parse("2026-04-10T00:00:00Z")),
            "security-audit",
            Timestamp.from(Instant.parse("2026-04-11T00:00:00Z")),
            "old remark",
            Timestamp.from(Instant.parse("2026-04-10T00:00:00Z")),
            Timestamp.from(Instant.parse("2026-04-11T00:00:00Z"))
        );

        adminAccountRepository.grantAdmin(LOGIN, "night-watch", "reactivated");

        assertThat(adminAccountRepository.isActiveAdmin(LOGIN)).isTrue();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_accounts WHERE user_login = ?",
                Integer.class,
                LOGIN
            )
        ).isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM admin_accounts WHERE user_login = ?",
                String.class,
                LOGIN
            )
        ).isEqualTo("ACTIVE");
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT revoked_by FROM admin_accounts WHERE user_login = ?",
                String.class,
                LOGIN
            )
        ).isNull();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT remark FROM admin_accounts WHERE user_login = ?",
                String.class,
                LOGIN
            )
        ).isEqualTo("reactivated");
    }
}
