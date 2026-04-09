package com.sharehub.auth;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class UserProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfileDto upsert(String login, String name, UUID avatarFileId) {
        Optional<UserProfileDto> existing = findOptional(login);
        if (existing.isPresent()) {
            UUID effectiveAvatarFileId = avatarFileId != null ? avatarFileId : existing.get().avatarFileId();
            jdbcTemplate.update(
                """
                    UPDATE users
                    SET name = ?, avatar_file_id = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE login = ?
                    """,
                nullable(name),
                effectiveAvatarFileId,
                login
            );
            return findByLogin(login);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO users (login, name, avatar_file_id, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, login);
                statement.setString(2, nullable(name));
                statement.setObject(3, avatarFileId);
                statement.setString(4, "ACTIVE");
                return statement;
            },
            keyHolder
        );
        return new UserProfileDto(
            keyHolder.getKey().longValue(),
            login,
            nullable(name),
            avatarFileId,
            avatarFileId == null ? null : "/api/files/" + avatarFileId,
            "ACTIVE"
        );
    }

    public UserProfileDto updateAvatar(String login, UUID avatarFileId) {
        ensureExists(login);
        jdbcTemplate.update(
            "UPDATE users SET avatar_file_id = ?, updated_at = CURRENT_TIMESTAMP WHERE login = ?",
            avatarFileId,
            login
        );
        return findByLogin(login);
    }

    public UserProfileDto findByLogin(String login) {
        return findOptional(login).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
    }

    public Optional<UserProfileDto> findOptionalByLogin(String login) {
        return findOptional(login);
    }

    public UserProfileDto findById(Long id) {
        return findOptionalById(id).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
    }

    public UserProfileDto updateStatus(Long id, String status) {
        int affected = jdbcTemplate.update("UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, id);
        if (affected == 0) {
            throw new NotFoundException("USER_NOT_FOUND");
        }
        return findById(id);
    }

    public PageResponse<UserProfileDto> listUsers(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        int offset = (safePage - 1) * safePageSize;
        List<UserProfileDto> items = jdbcTemplate.query(
            """
                SELECT id, login, name, avatar_file_id, status
                FROM users
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> mapUser(resultSet),
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public void ensureActive(String login) {
        UserProfileDto profile = findByLogin(login);
        if ("BANNED".equalsIgnoreCase(profile.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USER_BANNED");
        }
    }

    private Optional<UserProfileDto> findOptional(String login) {
        List<UserProfileDto> results = jdbcTemplate.query(
            """
                SELECT id, login, name, avatar_file_id, status
                FROM users
            WHERE login = ?
            """,
            (resultSet, rowNum) -> mapUser(resultSet),
            login
        );
        return results.stream().findFirst();
    }

    private void ensureExists(String login) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE login = ?", Integer.class, login);
        if (count == null || count == 0) {
            throw new NotFoundException("USER_NOT_FOUND");
        }
    }

    private UserProfileDto mapUser(ResultSet resultSet) throws SQLException {
        UUID avatarFileId = resultSet.getObject("avatar_file_id", UUID.class);
        return new UserProfileDto(
            resultSet.getLong("id"),
            resultSet.getString("login"),
            resultSet.getString("name"),
            avatarFileId,
            avatarFileId == null ? null : "/api/files/" + avatarFileId,
            resultSet.getString("status")
        );
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Optional<UserProfileDto> findOptionalById(Long id) {
        List<UserProfileDto> results = jdbcTemplate.query(
            """
                SELECT id, login, name, avatar_file_id, status
                FROM users
                WHERE id = ?
                """,
            (resultSet, rowNum) -> mapUser(resultSet),
            id
        );
        return results.stream().findFirst();
    }
}
