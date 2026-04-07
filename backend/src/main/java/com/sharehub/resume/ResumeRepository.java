package com.sharehub.resume;

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

@Repository
public class ResumeRepository {

    private static final String DEFAULT_OWNER_KEY = "local-dev-user";

    private final JdbcTemplate jdbcTemplate;

    public ResumeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumeDto create(String templateKey, UUID fileId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO resumes (template_key, owner_key, status, file_id, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, templateKey);
                statement.setString(2, DEFAULT_OWNER_KEY);
                statement.setString(3, "GENERATED");
                statement.setObject(4, fileId);
                return statement;
            },
            keyHolder
        );
        return find(keyHolder.getKey().longValue());
    }

    public PageResponse<ResumeDto> list(String ownerKey, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resumes WHERE owner_key = ?", Long.class, ownerKey);
        int offset = (safePage - 1) * safePageSize;
        List<ResumeDto> items = jdbcTemplate.query(
            """
                SELECT id, template_key, status, file_id
                FROM resumes
                WHERE owner_key = ?
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> mapResume(resultSet),
            ownerKey,
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public ResumeDto find(Long id) {
        return findOptional(id).orElseThrow(() -> new NotFoundException("RESUME_NOT_FOUND"));
    }

    public ResumeDto findOwned(Long id, String ownerKey) {
        return findOptionalOwned(id, ownerKey).orElseThrow(() -> new NotFoundException("RESUME_NOT_FOUND"));
    }

    public void delete(Long id, String ownerKey) {
        int affected = jdbcTemplate.update("DELETE FROM resumes WHERE id = ? AND owner_key = ?", id, ownerKey);
        if (affected == 0) {
            throw new NotFoundException("RESUME_NOT_FOUND");
        }
    }

    public long countByOwner(String ownerKey) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resumes WHERE owner_key = ?", Long.class, ownerKey);
        return count == null ? 0L : count;
    }

    private Optional<ResumeDto> findOptional(Long id) {
        List<ResumeDto> results = jdbcTemplate.query(
            """
                SELECT id, template_key, status, file_id
                FROM resumes
                WHERE id = ?
                """,
            (resultSet, rowNum) -> mapResume(resultSet),
            id
        );
        return results.stream().findFirst();
    }

    private Optional<ResumeDto> findOptionalOwned(Long id, String ownerKey) {
        List<ResumeDto> results = jdbcTemplate.query(
            """
                SELECT id, template_key, status, file_id
                FROM resumes
                WHERE id = ? AND owner_key = ?
                """,
            (resultSet, rowNum) -> mapResume(resultSet),
            id,
            ownerKey
        );
        return results.stream().findFirst();
    }

    private ResumeDto mapResume(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        UUID fileId = resultSet.getObject("file_id", UUID.class);
        return new ResumeDto(
            id,
            resultSet.getString("template_key"),
            resultSet.getString("status"),
            fileId,
            "/api/resumes/" + id + "/download"
        );
    }
}
