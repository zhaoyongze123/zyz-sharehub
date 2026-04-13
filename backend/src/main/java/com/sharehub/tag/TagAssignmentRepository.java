package com.sharehub.tag;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TagAssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public TagAssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> normalizeTags(Collection<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }
            String trimmed = rawTag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed);
        }
        return List.copyOf(normalized);
    }

    public void syncResourceTags(Long resourceId, Collection<String> rawTags) {
        syncAssignments("resource_tags", "resource_id", resourceId, rawTags);
    }

    public void syncNoteTags(Long noteId, Collection<String> rawTags) {
        syncAssignments("note_tags", "note_id", noteId, rawTags);
    }

    public Map<Long, List<String>> findResourceTagsByResourceIds(Collection<Long> resourceIds) {
        return findTagsByTargetIds("resource_tags", "resource_id", resourceIds);
    }

    public Map<Long, List<String>> findNoteTagsByNoteIds(Collection<Long> noteIds) {
        return findTagsByTargetIds("note_tags", "note_id", noteIds);
    }

    public Set<Long> findResourceIdsByTag(String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return Set.of();
        }
        String normalizedName = rawTag.trim().toLowerCase(Locale.ROOT);
        String slug = toSlug(rawTag);
        List<Long> ids = jdbcTemplate.query(
            """
                SELECT DISTINCT rt.resource_id
                FROM resource_tags rt
                JOIN tags t ON t.id = rt.tag_id
                WHERE lower(t.name) = ? OR t.slug = ?
                """,
            (resultSet, rowNum) -> resultSet.getLong("resource_id"),
            normalizedName,
            slug
        );
        return new LinkedHashSet<>(ids);
    }

    private void syncAssignments(String tableName, String targetColumn, Long targetId, Collection<String> rawTags) {
        if (targetId == null) {
            return;
        }
        List<String> normalizedTags = normalizeTags(rawTags);
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE " + targetColumn + " = ?", targetId);
        if (normalizedTags.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (String tagName : normalizedTags) {
            Long tagId = findOrCreateTagId(tagName, now);
            jdbcTemplate.update(
                "INSERT INTO " + tableName + " (" + targetColumn + ", tag_id, created_at) VALUES (?, ?, ?)",
                targetId,
                tagId,
                Timestamp.from(now)
            );
        }
    }

    private Long findOrCreateTagId(String tagName, Instant now) {
        String slug = toSlug(tagName);
        List<Long> existing = jdbcTemplate.query(
            "SELECT id FROM tags WHERE slug = ?",
            (resultSet, rowNum) -> resultSet.getLong("id"),
            slug
        );
        if (!existing.isEmpty()) {
            Long tagId = existing.get(0);
            jdbcTemplate.update(
                "UPDATE tags SET name = ?, updated_at = ? WHERE id = ?",
                tagName,
                Timestamp.from(now),
                tagId
            );
            return tagId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                    INSERT INTO tags (name, slug, type, status, created_at, updated_at)
                    VALUES (?, ?, 'CONTENT', 'ACTIVE', ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, tagName);
            statement.setString(2, slug);
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setTimestamp(4, Timestamp.from(now));
            return statement;
        }, keyHolder);
        Map<String, Object> keys = keyHolder.getKeys();
        Number key = null;
        if (keys != null) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number number) {
                key = number;
            }
        }
        if (key == null) {
            key = keyHolder.getKey();
        }
        if (key == null) {
            throw new IllegalStateException("TAG_ID_NOT_GENERATED");
        }
        return key.longValue();
    }

    private Map<Long, List<String>> findTagsByTargetIds(String tableName, String targetColumn, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Map.of();
        }
        List<Long> safeIds = targetIds.stream()
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = safeIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
            SELECT assignment.%s AS target_id, t.name
            FROM %s assignment
            JOIN tags t ON t.id = assignment.tag_id
            WHERE assignment.%s IN (%s)
            ORDER BY assignment.%s, lower(t.name), t.id
            """.formatted(targetColumn, tableName, targetColumn, placeholders, targetColumn);

        Map<Long, List<String>> result = new LinkedHashMap<>();
        jdbcTemplate.query(
            sql,
            resultSet -> {
                Long targetId = resultSet.getLong("target_id");
                result.computeIfAbsent(targetId, ignored -> new ArrayList<>()).add(resultSet.getString("name"));
            },
            safeIds.toArray()
        );
        return result;
    }

    private String toSlug(String rawTag) {
        String normalized = rawTag == null ? "" : rawTag.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[\\s_]+", "-");
        normalized = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}-]+", "");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isEmpty() ? "tag" : normalized;
    }
}
