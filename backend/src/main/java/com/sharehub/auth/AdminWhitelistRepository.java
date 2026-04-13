package com.sharehub.auth;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminWhitelistRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminWhitelistRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isAdmin(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_whitelist WHERE github_login = ? AND role = ?",
            Integer.class,
            login,
            "SUPER_ADMIN"
        );
        return count != null && count > 0;
    }

    public void grantSuperAdmin(String login, String createdBy) {
        String operator = normalizeOperator(createdBy);
        int updated = jdbcTemplate.update(
            """
                UPDATE admin_whitelist
                SET role = 'SUPER_ADMIN', created_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE github_login = ?
                """,
            operator,
            login
        );
        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO admin_whitelist (github_login, role, created_by, created_at, updated_at)
                    VALUES (?, 'SUPER_ADMIN', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                login,
                operator
            );
        }
    }

    public Optional<String> findRole(String login) {
        return jdbcTemplate.query(
            "SELECT role FROM admin_whitelist WHERE github_login = ?",
            (resultSet, rowNum) -> resultSet.getString("role"),
            login
        ).stream().findFirst();
    }

    private String normalizeOperator(String createdBy) {
        if (createdBy == null || createdBy.isBlank()) {
            return "system";
        }
        return createdBy;
    }
}
