package com.sharehub.interaction;

import com.sharehub.admin.AdminAuditLogDto;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.note.NoteRepository;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resource.ResourceRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class InteractionRepository {

    private static final String DEFAULT_OPERATOR_KEY = "system-admin";

    private final JdbcTemplate jdbcTemplate;
    private final ResourceRepository resourceRepository;
    private final NoteRepository noteRepository;

    public InteractionRepository(
        JdbcTemplate jdbcTemplate,
        ResourceRepository resourceRepository,
        NoteRepository noteRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceRepository = resourceRepository;
        this.noteRepository = noteRepository;
    }

    private static final String STATUS_VISIBLE = "VISIBLE";
    private static final String STATUS_HIDDEN = "HIDDEN";

    public CommentRecord saveComment(Long resourceId, String content, Long parentId, String authorKey) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMMENT_CONTENT_REQUIRED");
        }

        Long effectiveResourceId = resourceId;
        Long noteId = null;
        if (parentId != null) {
            ParentTarget parentTarget = findParentTarget(parentId);
            effectiveResourceId = parentTarget.resourceId();
            noteId = parentTarget.noteId();
        } else {
            requireResource(resourceId);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long finalResourceId = effectiveResourceId;
        Long finalNoteId = noteId;
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO comments (resource_id, note_id, parent_id, author_key, content, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[] {"id"}
                );
                bindLong(statement, 1, finalResourceId);
                bindLong(statement, 2, finalNoteId);
                bindLong(statement, 3, parentId);
                statement.setString(4, authorKey);
                statement.setString(5, content);
                statement.setString(6, STATUS_VISIBLE);
                statement.setTimestamp(7, Timestamp.from(Instant.now()));
                return statement;
            },
            keyHolder
        );
        return findComment(keyHolder.getKey().longValue());
    }

    public int addFavorite(Long resourceId, String userKey) {
        requireResource(resourceId);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            Integer.class,
            resourceId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO favorites (resource_id, note_id, user_key, created_at) VALUES (?, NULL, ?, CURRENT_TIMESTAMP)",
                resourceId,
                userKey
            );
        }
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE resource_id = ? AND note_id IS NULL",
            Integer.class,
            resourceId
        );
        return total == null ? 0 : total;
    }

    public int removeFavorite(Long resourceId, String userKey) {
        requireResource(resourceId);
        jdbcTemplate.update(
            "DELETE FROM favorites WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            resourceId,
            userKey
        );
        return (int) count("SELECT COUNT(*) FROM favorites WHERE resource_id = ? AND note_id IS NULL", resourceId);
    }

    public List<CommentNodeDto> listCommentTreeByResource(Long resourceId) {
        List<CommentRecord> records = jdbcTemplate.query(
            """
                SELECT id, resource_id, note_id, parent_id, content, status
                FROM comments
                WHERE resource_id = ?
                  AND status = 'VISIBLE'
                ORDER BY created_at ASC, id ASC
                """,
            (resultSet, rowNum) -> new CommentRecord(
                resultSet.getLong("id"),
                nullableLong(resultSet, "resource_id"),
                nullableLong(resultSet, "note_id"),
                nullableLong(resultSet, "parent_id"),
                resultSet.getString("content"),
                resultSet.getString("status")
            ),
            resourceId
        );
        Map<Long, CommentNodeDto> byId = new LinkedHashMap<>();
        for (CommentRecord record : records) {
            byId.put(record.id(), CommentNodeDto.from(record));
        }
        List<CommentNodeDto> roots = new ArrayList<>();
        for (CommentNodeDto node : byId.values()) {
            if (node.parentId() == null) {
                roots.add(node);
                continue;
            }
            CommentNodeDto parent = byId.get(node.parentId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            parent.children().add(node);
        }
        return roots;
    }

    public InteractionSummaryDto summarizeResource(Long resourceId) {
        return new InteractionSummaryDto(
            resourceId,
            count("SELECT COUNT(*) FROM comments WHERE resource_id = ? AND status = 'VISIBLE'", resourceId),
            count("SELECT COUNT(*) FROM favorites WHERE resource_id = ? AND note_id IS NULL", resourceId),
            count("SELECT COUNT(*) FROM likes WHERE resource_id = ? AND note_id IS NULL", resourceId),
            count("SELECT COUNT(*) FROM reports WHERE target_type = 'RESOURCE' AND target_id = ?", resourceId)
        );
    }

    public Map<Long, InteractionSummaryDto> summarizeResources(Collection<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(resourceIds));
        Map<Long, Long> commentCounts = queryResourceCountMap(
            "SELECT resource_id, COUNT(*) AS cnt FROM comments WHERE status = 'VISIBLE' AND resource_id IN (%s) GROUP BY resource_id",
            ids
        );
        Map<Long, Long> favoriteCounts = queryResourceCountMap(
            "SELECT resource_id, COUNT(*) AS cnt FROM favorites WHERE note_id IS NULL AND resource_id IN (%s) GROUP BY resource_id",
            ids
        );
        Map<Long, Long> likeCounts = queryResourceCountMap(
            "SELECT resource_id, COUNT(*) AS cnt FROM likes WHERE note_id IS NULL AND resource_id IN (%s) GROUP BY resource_id",
            ids
        );
        Map<Long, Long> reportCounts = queryTargetCountMap(
            "SELECT target_id, COUNT(*) AS cnt FROM reports WHERE target_type = 'RESOURCE' AND target_id IN (%s) GROUP BY target_id",
            ids
        );

        Map<Long, InteractionSummaryDto> result = new HashMap<>();
        for (Long resourceId : ids) {
            result.put(
                resourceId,
                new InteractionSummaryDto(
                    resourceId,
                    commentCounts.getOrDefault(resourceId, 0L),
                    favoriteCounts.getOrDefault(resourceId, 0L),
                    likeCounts.getOrDefault(resourceId, 0L),
                    reportCounts.getOrDefault(resourceId, 0L)
                )
            );
        }
        return result;
    }

    private Map<Long, Long> queryResourceCountMap(String sqlTemplate, List<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", resourceIds.stream().map(id -> "?").toList());
        String sql = sqlTemplate.formatted(placeholders);
        List<Map.Entry<Long, Long>> rows = jdbcTemplate.query(
            sql,
            (resultSet, rowNum) -> Map.entry(resultSet.getLong(1), resultSet.getLong(2)),
            resourceIds.toArray()
        );
        Map<Long, Long> counts = new HashMap<>();
        for (Map.Entry<Long, Long> row : rows) {
            counts.put(row.getKey(), row.getValue());
        }
        return counts;
    }

    private Map<Long, Long> queryTargetCountMap(String sqlTemplate, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", targetIds.stream().map(id -> "?").toList());
        String sql = sqlTemplate.formatted(placeholders);
        List<Map.Entry<Long, Long>> rows = jdbcTemplate.query(
            sql,
            (resultSet, rowNum) -> Map.entry(resultSet.getLong(1), resultSet.getLong(2)),
            targetIds.toArray()
        );
        Map<Long, Long> counts = new HashMap<>();
        for (Map.Entry<Long, Long> row : rows) {
            counts.put(row.getKey(), row.getValue());
        }
        return counts;
    }

    public CommentRecord hideComment(Long commentId) {
        return updateCommentStatus(commentId, STATUS_HIDDEN);
    }

    public CommentRecord unhideComment(Long commentId) {
        return updateCommentStatus(commentId, STATUS_VISIBLE);
    }

    public CommentRecord updateCommentStatus(Long commentId, String status) {
        int affected = jdbcTemplate.update("UPDATE comments SET status = ? WHERE id = ?", status, commentId);
        if (affected == 0) {
            throw new NotFoundException("COMMENT_NOT_FOUND");
        }
        return findComment(commentId);
    }

    public PageResponse<ResourceDto> listFavoriteResources(String userKey, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM favorites f
                JOIN resources r ON r.id = f.resource_id
                WHERE f.user_key = ? AND f.resource_id IS NOT NULL
                """,
            Long.class,
            userKey
        );
        int offset = (safePage - 1) * safePageSize;
        List<ResourceDto> items = jdbcTemplate.query(
            """
                SELECT r.id, r.title, r.type, r.summary, r.tags, r.external_url, r.object_key, r.visibility, r.status
                FROM favorites f
                JOIN resources r ON r.id = f.resource_id
                WHERE f.user_key = ? AND f.resource_id IS NOT NULL
                ORDER BY f.id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> new ResourceDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("type"),
                resultSet.getString("summary"),
                splitTags(resultSet.getString("tags")),
                resultSet.getString("external_url"),
                resultSet.getString("object_key"),
                resultSet.getString("visibility"),
                resultSet.getString("status")
            ),
            userKey,
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public int addLike(Long resourceId, String userKey) {
        requireResource(resourceId);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM likes WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            Integer.class,
            resourceId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO likes (resource_id, note_id, user_key, created_at) VALUES (?, NULL, ?, CURRENT_TIMESTAMP)",
                resourceId,
                userKey
            );
        }
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM likes WHERE resource_id = ? AND note_id IS NULL",
            Integer.class,
            resourceId
        );
        return total == null ? 0 : total;
    }

    public int removeLike(Long resourceId, String userKey) {
        requireResource(resourceId);
        jdbcTemplate.update(
            "DELETE FROM likes WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            resourceId,
            userKey
        );
        return (int) count("SELECT COUNT(*) FROM likes WHERE resource_id = ? AND note_id IS NULL", resourceId);
    }

    public ReportRecord saveReport(Long resourceId, String reason, String reporter) {
        requireResource(resourceId);
        return insertReport("RESOURCE", resourceId, reason, reporter);
    }

    public ReportRecord saveNoteReport(Long noteId, String reason, String reporter) {
        requireNote(noteId);
        return insertReport("NOTE", noteId, reason, reporter);
    }

    private ReportRecord insertReport(String targetType, Long targetId, String reason, String reporter) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO reports (target_type, target_id, reporter_key, reason, details, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, targetType);
                statement.setLong(2, targetId);
                statement.setString(3, reporter);
                statement.setString(4, reason == null || reason.isBlank() ? "无" : reason);
                statement.setString(5, null);
                statement.setString(6, "OPEN");
                statement.setTimestamp(7, Timestamp.from(Instant.now()));
                return statement;
            },
            keyHolder
        );
        return findReport(keyHolder.getKey().longValue());
    }

    public PageResponse<ReportRecord> listReports() {
        return findReports(1, 20, null, null);
    }

    public PageResponse<ReportRecord> findReports(int page, int pageSize, String statusFilter, String targetTypeFilter) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, pageSize);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !statusFilter.isBlank()) {
            where.append(" AND status = ?");
            params.add(statusFilter);
        }
        if (targetTypeFilter != null && !targetTypeFilter.isBlank()) {
            where.append(" AND target_type = ?");
            params.add(targetTypeFilter);
        }
        String countSql = "SELECT COUNT(*) FROM reports" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        int offset = (safePage - 1) * safeSize;
        StringBuilder query = new StringBuilder(
            """
                SELECT id, target_type, target_id, reporter_key, reason, status
                FROM reports
                """
        );
        query.append(where);
        query.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(safeSize);
        params.add(offset);
        List<ReportRecord> items = jdbcTemplate.query(
            query.toString(),
            (resultSet, rowNum) -> mapReport(resultSet),
            params.toArray()
        );
        return PageResponse.of(items, safePage, safeSize, total == null ? 0L : total);
    }

    public ReportRecord resolveReport(Long id) {
        int updated = jdbcTemplate.update(
            """
                UPDATE reports
                SET status = ?, resolved_at = ?, resolved_by = ?
                WHERE id = ?
                """,
            "RESOLVED",
            Timestamp.from(Instant.now()),
            DEFAULT_OPERATOR_KEY,
            id
        );
        if (updated == 0) {
            throw new NotFoundException("REPORT_NOT_FOUND");
        }
        appendAuditLog("RESOLVE_REPORT", "REPORT", String.valueOf(id), "{}");
        return findReport(id);
    }

    public void appendAuditLog(String action, String targetType, String targetId, String details) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_audit_logs (action, target_type, target_id, operator_key, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
            action,
            targetType,
            targetId,
            DEFAULT_OPERATOR_KEY,
            details == null ? "{}" : details,
            Timestamp.from(Instant.now())
        );
    }

    public PageResponse<AdminAuditLogDto> listAuditLogs(int page, int pageSize, String actionFilter, String targetTypeFilter) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (actionFilter != null && !actionFilter.isBlank()) {
            where.append(" AND action = ?");
            params.add(actionFilter);
        }
        if (targetTypeFilter != null && !targetTypeFilter.isBlank()) {
            where.append(" AND target_type = ?");
            params.add(targetTypeFilter);
        }
        String countSql = "SELECT COUNT(*) FROM admin_audit_logs" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        int offset = (safePage - 1) * safePageSize;
        StringBuilder query = new StringBuilder(
            """
                SELECT id, action, target_type, target_id, operator_key, details, created_at
                FROM admin_audit_logs
                """
        );
        query.append(where);
        query.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(safePageSize);
        params.add(offset);
        List<AdminAuditLogDto> items = jdbcTemplate.query(
            query.toString(),
            (resultSet, rowNum) -> new AdminAuditLogDto(
                resultSet.getLong("id"),
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("operator_key"),
                resultSet.getString("details"),
                resultSet.getTimestamp("created_at").toInstant()
            ),
            params.toArray()
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    private ParentTarget findParentTarget(Long parentId) {
        List<ParentTarget> results = jdbcTemplate.query(
            "SELECT resource_id, note_id FROM comments WHERE id = ?",
            (resultSet, rowNum) -> new ParentTarget(
                nullableLong(resultSet, "resource_id"),
                nullableLong(resultSet, "note_id")
            ),
            parentId
        );
        return results.stream().findFirst().orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND"));
    }

    private CommentRecord findComment(Long id) {
        List<CommentRecord> results = jdbcTemplate.query(
            """
                SELECT id, resource_id, note_id, parent_id, content, status
                FROM comments
                WHERE id = ?
                """,
            (resultSet, rowNum) -> new CommentRecord(
                resultSet.getLong("id"),
                nullableLong(resultSet, "resource_id"),
                nullableLong(resultSet, "note_id"),
                nullableLong(resultSet, "parent_id"),
                resultSet.getString("content"),
                resultSet.getString("status")
            ),
            id
        );
        return results.stream().findFirst().orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND"));
    }

    private ReportRecord findReport(Long id) {
        List<ReportRecord> results = jdbcTemplate.query(
            """
                SELECT id, target_type, target_id, reporter_key, reason, status
                FROM reports
                WHERE id = ?
                """,
            (resultSet, rowNum) -> mapReport(resultSet),
            id
        );
        return results.stream().findFirst().orElseThrow(() -> new NotFoundException("REPORT_NOT_FOUND"));
    }

    private ReportRecord mapReport(ResultSet resultSet) throws SQLException {
        return new ReportRecord(
            resultSet.getLong("id"),
            resultSet.getString("target_type"),
            resultSet.getLong("target_id"),
            resultSet.getString("reason"),
            resultSet.getString("reporter_key"),
            resultSet.getString("status")
        );
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        return value == null ? null : resultSet.getLong(column);
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.asList(tags.split(","));
    }

    private long count(String sql, Long resourceId) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, resourceId);
        return value == null ? 0L : value;
    }

    private void requireResource(Long resourceId) {
        if (resourceId == null || !resourceRepository.existsById(resourceId)) {
            throw new NotFoundException("RESOURCE_NOT_FOUND");
        }
    }

    private void requireNote(Long noteId) {
        if (noteId == null || !noteRepository.existsById(noteId)) {
            throw new NotFoundException("NOTE_NOT_FOUND");
        }
    }

    private void bindLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private record ParentTarget(Long resourceId, Long noteId) {}

    public record CommentRecord(Long id, Long resourceId, Long noteId, Long parentId, String content, String status) {}

    public record ReportRecord(Long id, String targetType, Long targetId, String reason, String reporter, String status) {}
}
