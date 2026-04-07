package com.sharehub.resume;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
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

    public PageResponse<ResumeDto> list(String ownerKey, int page, int pageSize, String statusFilter) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String baseFilter = "WHERE owner_key = ?";
        List<Object> paramList = new ArrayList<>();
        paramList.add(ownerKey);
        if (statusFilter != null && !statusFilter.isBlank()) {
            baseFilter += " AND status = ?";
            paramList.add(statusFilter);
        }

        String countSql = "SELECT COUNT(*) FROM resumes " + baseFilter;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, paramList.toArray());
        int offset = (safePage - 1) * safePageSize;
        StringBuilder query = new StringBuilder(
            """
                SELECT r.id, r.template_key, r.status, r.file_id, f.filename, f.size, f.created_at AS file_created_at, f.updated_at AS file_updated_at
                FROM resumes r
                LEFT JOIN files f ON r.file_id = f.id
                """
        );
        query.append(baseFilter);
        query.append(" ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
        paramList.add(safePageSize);
        paramList.add(offset);
        List<ResumeDto> items = jdbcTemplate.query(
            query.toString(),
            (resultSet, rowNum) -> mapResume(resultSet),
            paramList.toArray()
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
                SELECT r.id, r.template_key, r.status, r.file_id, f.filename, f.size, f.created_at AS file_created_at, f.updated_at AS file_updated_at
                FROM resumes r
                LEFT JOIN files f ON r.file_id = f.id
                WHERE r.id = ?
                """,
            (resultSet, rowNum) -> mapResume(resultSet),
            id
        );
        return results.stream().findFirst();
    }

    private Optional<ResumeDto> findOptionalOwned(Long id, String ownerKey) {
        List<ResumeDto> results = jdbcTemplate.query(
            """
                SELECT r.id, r.template_key, r.status, r.file_id, f.filename, f.size, f.created_at AS file_created_at, f.updated_at AS file_updated_at
                FROM resumes r
                LEFT JOIN files f ON r.file_id = f.id
                WHERE r.id = ? AND r.owner_key = ?
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
        String fileName = resultSet.getString("filename");
        long fileSize = resultSet.getLong("size");
        Instant fileCreatedAt = resultSet.getObject("file_created_at", Instant.class);
        Instant fileUpdatedAt = resultSet.getObject("file_updated_at", Instant.class);
        return new ResumeDto(
            id,
            resultSet.getString("template_key"),
            resultSet.getString("status"),
            fileId,
            "/api/resumes/" + id + "/download"
            ,
            fileName,
            fileSize == 0 && resultSet.wasNull() ? null : fileSize,
            fileCreatedAt,
            fileUpdatedAt
        );
    }
}
