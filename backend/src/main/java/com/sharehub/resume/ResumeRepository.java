package com.sharehub.resume;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

    private static final String DEFAULT_WORKBENCH_STATUS = "GENERATED";
    private static final int RECENT_LIMIT = 5;

    private final JdbcTemplate jdbcTemplate;

    public ResumeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumeDto create(String ownerKey, String templateKey, UUID fileId) {
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
                statement.setString(2, ownerKey);
                statement.setString(3, DEFAULT_WORKBENCH_STATUS);
                statement.setObject(4, fileId, Types.OTHER);
                return statement;
            },
            keyHolder
        );
        return find(keyHolder.getKey().longValue());
    }

    public PageResponse<ResumeDto> list(
        String ownerKey,
        int page,
        int pageSize,
        String statusFilter,
        String templateKeyFilter,
        String keyword
    ) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String baseFilter = "WHERE r.owner_key = ?";
        List<Object> paramList = new ArrayList<>();
        paramList.add(ownerKey);
        if (statusFilter != null && !statusFilter.isBlank()) {
            baseFilter += " AND r.status = ?";
            paramList.add(statusFilter);
        }
        if (templateKeyFilter != null && !templateKeyFilter.isBlank()) {
            baseFilter += " AND r.template_key = ?";
            paramList.add(templateKeyFilter);
        }
        if (keyword != null && !keyword.isBlank()) {
            baseFilter += " AND (LOWER(r.template_key) LIKE ? OR LOWER(COALESCE(f.filename, '')) LIKE ?)";
            String likeValue = "%" + keyword.trim().toLowerCase() + "%";
            paramList.add(likeValue);
            paramList.add(likeValue);
        }

        String countSql = """
            SELECT COUNT(*)
            FROM resumes r
            LEFT JOIN files f ON r.file_id = f.id
            """ + baseFilter;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, paramList.toArray());

        int offset = (safePage - 1) * safePageSize;
        List<Object> queryParams = new ArrayList<>(paramList);
        queryParams.add(safePageSize);
        queryParams.add(offset);
        String sql = """
            SELECT r.id, r.template_key, r.status, r.file_id, f.filename, f.size, f.created_at AS file_created_at, f.updated_at AS file_updated_at
            FROM resumes r
            LEFT JOIN files f ON r.file_id = f.id
            """ + baseFilter + " ORDER BY r.created_at DESC LIMIT ? OFFSET ?";
        List<ResumeDto> items = jdbcTemplate.query(
            sql,
            (resultSet, rowNum) -> mapResume(resultSet),
            queryParams.toArray()
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public PageResponse<ResumeDto> list(String ownerKey, int page, int pageSize, String statusFilter) {
        return list(ownerKey, page, pageSize, statusFilter, null, null);
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

    public ResumeWorkbenchDto workbench(String ownerKey) {
        long total = countByOwner(ownerKey);
        long generatedCount = countByOwnerAndStatus(ownerKey, DEFAULT_WORKBENCH_STATUS);
        List<ResumeTemplateBreakdownDto> templates = jdbcTemplate.query(
            """
                SELECT template_key, COUNT(*) AS total
                FROM resumes
                WHERE owner_key = ?
                GROUP BY template_key
                ORDER BY total DESC
                """,
            (rs, rowNum) -> new ResumeTemplateBreakdownDto(rs.getString("template_key"), rs.getLong("total")),
            ownerKey
        );
        List<ResumeDto> recentItems = jdbcTemplate.query(
            """
                SELECT r.id, r.template_key, r.status, r.file_id, f.filename, f.size, f.created_at AS file_created_at, f.updated_at AS file_updated_at
                FROM resumes r
                LEFT JOIN files f ON r.file_id = f.id
                WHERE r.owner_key = ?
                ORDER BY r.created_at DESC
                LIMIT ?
                """,
            (rs, rowNum) -> mapResume(rs),
            ownerKey,
            RECENT_LIMIT
        );
        return new ResumeWorkbenchDto(total, generatedCount, templates, recentItems);
    }

    private long countByOwnerAndStatus(String ownerKey, String status) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resumes WHERE owner_key = ? AND status = ?",
            Long.class,
            ownerKey,
            status
        );
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
            "/api/resumes/" + id + "/download",
            fileName,
            fileSize == 0 && resultSet.wasNull() ? null : fileSize,
            fileCreatedAt,
            fileUpdatedAt
        );
    }
}
