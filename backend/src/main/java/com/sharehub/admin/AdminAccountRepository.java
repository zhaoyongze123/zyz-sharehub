package com.sharehub.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isActiveAdmin(String userLogin) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM admin_accounts
                WHERE user_login = ? AND status = 'ACTIVE'
                """,
            Integer.class,
            userLogin
        );
        return count != null && count > 0;
    }

    public void grantAdmin(String userLogin, String grantedBy, String remark) {
        Timestamp now = Timestamp.from(Instant.now());
        int affected = jdbcTemplate.update(
            """
                UPDATE admin_accounts
                SET status = 'ACTIVE', granted_by = ?, granted_at = ?, revoked_by = NULL, revoked_at = NULL, remark = ?, updated_at = ?
                WHERE user_login = ?
                """,
            grantedBy,
            now,
            remark,
            now,
            userLogin
        );
        if (affected > 0) {
            return;
        }

        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                user_login, status, granted_by, granted_at, remark, created_at, updated_at
                ) VALUES (?, 'ACTIVE', ?, ?, ?, ?, ?)
                """,
            userLogin,
            grantedBy,
            now,
            remark,
            now,
            now
        );
    }

    public List<AdminAccountRecord> listActiveAdmins() {
        return jdbcTemplate.query(
            """
                SELECT id, user_login, status, granted_by, granted_at, remark
                FROM admin_accounts
                WHERE status = 'ACTIVE'
                ORDER BY user_login ASC
                """,
            (resultSet, rowNum) -> mapRecord(resultSet)
        );
    }

    private AdminAccountRecord mapRecord(ResultSet resultSet) throws SQLException {
        return new AdminAccountRecord(
            resultSet.getLong("id"),
            resultSet.getString("user_login"),
            resultSet.getString("status"),
            resultSet.getString("granted_by"),
            resultSet.getTimestamp("granted_at") == null ? null : resultSet.getTimestamp("granted_at").toInstant(),
            resultSet.getString("remark")
        );
    }

    public record AdminAccountRecord(
        Long id,
        String userLogin,
        String status,
        String grantedBy,
        Instant grantedAt,
        String remark
    ) {}
}
