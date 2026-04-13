package com.sharehub.interaction;

import com.sharehub.admin.AdminAuditLogDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.note.RelatedNoteDto;
import com.sharehub.note.NoteRepository;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resource.ResourceRepository;
import com.sharehub.tag.TagAssignmentRepository;
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
    private final TagAssignmentRepository tagAssignmentRepository;
    private final UserProfileRepository userProfileRepository;

    public InteractionRepository(
        JdbcTemplate jdbcTemplate,
        ResourceRepository resourceRepository,
        NoteRepository noteRepository,
        TagAssignmentRepository tagAssignmentRepository,
        UserProfileRepository userProfileRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceRepository = resourceRepository;
        this.noteRepository = noteRepository;
        this.tagAssignmentRepository = tagAssignmentRepository;
        this.userProfileRepository = userProfileRepository;
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
        Long authorUserId = resolveUserId(authorKey);
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO comments (resource_id, note_id, parent_id, author_key, user_id, content, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[] {"id"}
                );
                bindLong(statement, 1, finalResourceId);
                bindLong(statement, 2, finalNoteId);
                bindLong(statement, 3, parentId);
                statement.setString(4, authorKey);
                statement.setObject(5, authorUserId);
                statement.setString(6, content);
                statement.setString(7, STATUS_VISIBLE);
                statement.setTimestamp(8, Timestamp.from(Instant.now()));
                return statement;
            },
            keyHolder
        );
        return findComment(keyHolder.getKey().longValue());
    }

    public int addFavorite(Long resourceId, String userKey) {
        requireResource(resourceId);
        Long userId = resolveUserId(userKey);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            Integer.class,
            resourceId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO favorites (resource_id, note_id, user_key, user_id, created_at) VALUES (?, NULL, ?, ?, CURRENT_TIMESTAMP)",
                resourceId,
                userKey,
                userId
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

    public int addNoteFavorite(Long noteId, String userKey) {
        requireAccessibleNote(noteId, userKey);
        Long userId = resolveUserId(userKey);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE note_id = ? AND resource_id IS NULL AND user_key = ?",
            Integer.class,
            noteId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO favorites (resource_id, note_id, user_key, user_id, created_at) VALUES (NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                noteId,
                userKey,
                userId
            );
        }
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE note_id = ? AND resource_id IS NULL",
            Integer.class,
            noteId
        );
        return total == null ? 0 : total;
    }

    public int removeNoteFavorite(Long noteId, String userKey) {
        requireAccessibleNote(noteId, userKey);
        jdbcTemplate.update(
            "DELETE FROM favorites WHERE note_id = ? AND resource_id IS NULL AND user_key = ?",
            noteId,
            userKey
        );
        return (int) countByNote("SELECT COUNT(*) FROM favorites WHERE note_id = ? AND resource_id IS NULL", noteId);
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
        Map<Long, InteractionSummaryDto> result = new HashMap<>();
        for (Long resourceId : resourceIds) {
            result.put(resourceId, summarizeResource(resourceId));
        }
        return result;
    }

    public NoteInteractionSummaryDto summarizeNote(Long noteId, String viewerKey) {
        requireAccessibleNote(noteId, viewerKey);
        return new NoteInteractionSummaryDto(
            noteId,
            countByNote("SELECT COUNT(*) FROM favorites WHERE note_id = ? AND resource_id IS NULL", noteId),
            countByNote("SELECT COUNT(*) FROM likes WHERE note_id = ? AND resource_id IS NULL", noteId),
            countByNote("SELECT COUNT(*) FROM reports WHERE target_type = 'NOTE' AND target_id = ?", noteId)
        );
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
                WHERE f.user_key = ? AND f.resource_id IS NOT NULL AND r.deleted_at IS NULL
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
                WHERE f.user_key = ? AND f.resource_id IS NOT NULL AND r.deleted_at IS NULL
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

    public PageResponse<RelatedNoteDto> listFavoriteNotes(String userKey, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM favorites f
                JOIN notes n ON n.id = f.note_id
                WHERE f.user_key = ? AND f.note_id IS NOT NULL AND f.resource_id IS NULL AND n.deleted_at IS NULL
                """,
            Long.class,
            userKey
        );
        int offset = (safePage - 1) * safePageSize;
        List<RelatedNoteDto> items = jdbcTemplate.query(
            """
                SELECT
                  n.id,
                  n.title,
                  n.content_md,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key) AS owner_name,
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END AS owner_avatar_url,
                  n.status,
                  n.category,
                  f.created_at,
                  COUNT(all_f.id) AS favorite_count
                FROM favorites f
                JOIN notes n ON n.id = f.note_id
                LEFT JOIN users u
                  ON u.id = n.user_id
                 OR (n.user_id IS NULL AND u.login = n.owner_key)
                LEFT JOIN favorites all_f ON all_f.note_id = n.id AND all_f.resource_id IS NULL
                WHERE f.user_key = ? AND f.note_id IS NOT NULL AND f.resource_id IS NULL AND n.deleted_at IS NULL
                GROUP BY
                  n.id,
                  n.title,
                  n.content_md,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key),
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END,
                  n.status,
                  n.category,
                  f.created_at
                ORDER BY f.created_at DESC, n.id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> new RelatedNoteDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                extractNoteSummary(resultSet.getString("content_md")),
                resultSet.getTimestamp("created_at") == null ? null : resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("category") == null ? List.of() : List.of(resultSet.getString("category")),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                resultSet.getLong("favorite_count"),
                true
            ),
            userKey,
            safePageSize,
            offset
        );
        Map<Long, List<String>> tagsByNoteId = tagAssignmentRepository.findNoteTagsByNoteIds(
            items.stream().map(RelatedNoteDto::id).toList()
        );
        items = items.stream()
            .map(item -> item.withTags(tagsByNoteId.getOrDefault(item.id(), List.of())))
            .toList();
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public long countFavoritedPublishedNotesByOwner(String ownerKey) {
        Long userId = resolveUserId(ownerKey);
        Long total = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM favorites f
                JOIN notes n ON n.id = f.note_id
                WHERE (%s)
                  AND n.status = 'PUBLISHED'
                  AND n.deleted_at IS NULL
                  AND f.note_id IS NOT NULL
                  AND f.resource_id IS NULL
                """.formatted(noteOwnerMatchClause(userId)),
            Long.class,
            noteOwnerArgs(userId, ownerKey)
        );
        return total == null ? 0L : total;
    }

    public int addLike(Long resourceId, String userKey) {
        requireResource(resourceId);
        Long userId = resolveUserId(userKey);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM likes WHERE resource_id = ? AND note_id IS NULL AND user_key = ?",
            Integer.class,
            resourceId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO likes (resource_id, note_id, user_key, user_id, created_at) VALUES (?, NULL, ?, ?, CURRENT_TIMESTAMP)",
                resourceId,
                userKey,
                userId
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

    public int addNoteLike(Long noteId, String userKey) {
        requireAccessibleNote(noteId, userKey);
        Long userId = resolveUserId(userKey);
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM likes WHERE note_id = ? AND resource_id IS NULL AND user_key = ?",
            Integer.class,
            noteId,
            userKey
        );
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO likes (resource_id, note_id, user_key, user_id, created_at) VALUES (NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                noteId,
                userKey,
                userId
            );
        }
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM likes WHERE note_id = ? AND resource_id IS NULL",
            Integer.class,
            noteId
        );
        return total == null ? 0 : total;
    }

    public int removeNoteLike(Long noteId, String userKey) {
        requireAccessibleNote(noteId, userKey);
        jdbcTemplate.update(
            "DELETE FROM likes WHERE note_id = ? AND resource_id IS NULL AND user_key = ?",
            noteId,
            userKey
        );
        return (int) countByNote("SELECT COUNT(*) FROM likes WHERE note_id = ? AND resource_id IS NULL", noteId);
    }

    public ReportRecord saveReport(Long resourceId, String reason, String reporter) {
        requireResource(resourceId);
        return insertReport("RESOURCE", resourceId, reason, reporter);
    }

    public ReportRecord saveNoteReport(Long noteId, String reason, String reporter) {
        requireAccessibleNote(noteId, reporter);
        return insertReport("NOTE", noteId, reason, reporter);
    }

    private ReportRecord insertReport(String targetType, Long targetId, String reason, String reporter) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long userId = resolveUserId(reporter);
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO reports (target_type, target_id, reporter_key, user_id, reason, details, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, targetType);
                statement.setLong(2, targetId);
                statement.setString(3, reporter);
                statement.setObject(4, userId);
                statement.setString(5, reason == null || reason.isBlank() ? "无" : reason);
                statement.setString(6, null);
                statement.setString(7, "OPEN");
                statement.setTimestamp(8, Timestamp.from(Instant.now()));
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

    private Long resolveUserId(String login) {
        return userProfileRepository.findIdOptionalByLogin(login).orElse(null);
    }

    private String noteOwnerMatchClause(Long userId) {
        if (userId != null) {
            return "n.user_id = ? OR (n.user_id IS NULL AND n.owner_key = ?)";
        }
        return "n.owner_key = ?";
    }

    private Object[] noteOwnerArgs(Long userId, String ownerKey) {
        if (userId != null) {
            return new Object[] {userId, ownerKey};
        }
        return new Object[] {ownerKey};
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.asList(tags.split(","));
    }

    private String extractNoteSummary(String contentMd) {
        if (contentMd == null || contentMd.isBlank()) {
            return "暂无摘要";
        }
        for (String line : contentMd.split("\\R")) {
            String normalized = line.trim();
            if (normalized.isEmpty() || normalized.startsWith("#")) {
                continue;
            }
            return normalized.length() > 96 ? normalized.substring(0, 96) + "..." : normalized;
        }
        String fallback = contentMd.replaceAll("\\s+", " ").trim();
        return fallback.length() > 96 ? fallback.substring(0, 96) + "..." : fallback;
    }

    private long count(String sql, Long resourceId) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, resourceId);
        return value == null ? 0L : value;
    }

    private long countByNote(String sql, Long noteId) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, noteId);
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

    private void requireAccessibleNote(Long noteId, String viewerKey) {
        if (noteId == null) {
            throw new NotFoundException("NOTE_NOT_FOUND");
        }
        noteRepository.findAccessible(noteId, viewerKey);
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
