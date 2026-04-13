package com.sharehub.note;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,.;:!?()\\[\\]{}，。；：！？、/\\\\|\\-]+");

    private final JdbcTemplate jdbcTemplate;

    public NoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public NoteDto save(String ownerKey, NoteDto note) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO notes (
                          title,
                          content_md,
                          owner_key,
                          visibility,
                          status,
                          category,
                          is_official,
                          is_pinned,
                          created_at,
                          updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, note.title());
                statement.setString(2, note.contentMd());
                statement.setString(3, ownerKey);
                statement.setString(4, nullable(note.visibility()));
                statement.setString(5, defaultStatus(note.status()));
                statement.setString(6, nullable(note.category()));
                statement.setBoolean(7, note.isOfficial());
                statement.setBoolean(8, note.isPinned());
                return statement;
            },
            keyHolder
        );
        return findOwned(keyHolder.getKey().longValue(), ownerKey);
    }

    public PageResponse<NoteDto> list(String ownerKey, int page, int pageSize) {
        return listByOwner(ownerKey, null, page, pageSize);
    }

    public PageResponse<NoteDto> listPublished(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM notes
                WHERE visibility = 'PUBLIC' AND status = 'PUBLISHED'
                """,
            Long.class
        );

        int offset = (safePage - 1) * safePageSize;
        List<NoteDto> items = jdbcTemplate.query(
            """
                SELECT
                  n.id,
                  n.title,
                  n.content_md,
                  n.visibility,
                  n.status,
                  n.category,
                  n.created_at,
                  n.updated_at,
                  n.is_official,
                  n.is_pinned,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key) AS owner_name,
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END AS owner_avatar_url
                FROM notes n
                LEFT JOIN users u ON u.login = n.owner_key
                WHERE n.visibility = 'PUBLIC' AND n.status = 'PUBLISHED'
                ORDER BY n.is_pinned DESC, n.id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at")),
                resultSet.getBoolean("is_official"),
                resultSet.getBoolean("is_pinned")
            ),
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public PageResponse<NoteDto> listByOwner(String ownerKey, String status, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String normalizedStatus = normalize(status);
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM notes WHERE owner_key = ?");
        List<Object> countArgs = new ArrayList<>();
        countArgs.add(ownerKey);
        if (normalizedStatus != null) {
            countSql.append(" AND status = ?");
            countArgs.add(normalizedStatus);
        }
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countArgs.toArray());

        int offset = (safePage - 1) * safePageSize;
        StringBuilder listSql = new StringBuilder(
            """
                SELECT
                  n.id,
                  n.title,
                  n.content_md,
                  n.visibility,
                  n.status,
                  n.category,
                  n.created_at,
                  n.updated_at,
                  n.is_official,
                  n.is_pinned,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key) AS owner_name,
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END AS owner_avatar_url
                FROM notes n
                LEFT JOIN users u ON u.login = n.owner_key
                WHERE n.owner_key = ?
                """
        );
        List<Object> listArgs = new ArrayList<>();
        listArgs.add(ownerKey);
        if (normalizedStatus != null) {
            listSql.append(" AND n.status = ?");
            listArgs.add(normalizedStatus);
        }
        listSql.append(" ORDER BY n.id DESC LIMIT ? OFFSET ?");
        listArgs.add(safePageSize);
        listArgs.add(offset);

        List<NoteDto> items = jdbcTemplate.query(
            listSql.toString(),
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at")),
                resultSet.getBoolean("is_official"),
                resultSet.getBoolean("is_pinned")
            ),
            listArgs.toArray()
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public NoteDto findOwned(Long id, String ownerKey) {
        return findOptional(id, ownerKey).orElseThrow(() -> new NotFoundException("NOTE_NOT_FOUND"));
    }

    public NoteDto findAccessible(Long id, String ownerKey) {
        if (ownerKey != null && !ownerKey.isBlank()) {
            Optional<NoteDto> owned = findOptional(id, ownerKey);
            if (owned.isPresent()) {
                return owned.get();
            }
        }
        return findPublicPublished(id).orElseThrow(() -> new NotFoundException("NOTE_NOT_FOUND"));
    }

    public NoteDto upsertOwned(Long id, String ownerKey, NoteDto note) {
        NoteDto existing = findOwned(id, ownerKey);
        jdbcTemplate.update(
            """
                UPDATE notes
                SET title = ?,
                    content_md = ?,
                    visibility = ?,
                    status = ?,
                    category = ?,
                    is_official = ?,
                    is_pinned = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND owner_key = ?
                """,
            note.title(),
            note.contentMd(),
            nullable(note.visibility()),
            normalizeForUpdate(note.status(), existing.status()),
            nullable(note.category()),
            note.isOfficial(),
            note.isPinned(),
            id,
            ownerKey
        );
        return findOwned(id, ownerKey);
    }

    public void deleteOwned(Long id, String ownerKey) {
        int affected = jdbcTemplate.update("DELETE FROM notes WHERE id = ? AND owner_key = ?", id, ownerKey);
        if (affected == 0) {
            throw new NotFoundException("NOTE_NOT_FOUND");
        }
    }

    public void deleteById(Long id) {
        int affected = jdbcTemplate.update("DELETE FROM notes WHERE id = ?", id);
        if (affected == 0) {
            throw new NotFoundException("NOTE_NOT_FOUND");
        }
    }

    public long countByOwner(String ownerKey) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE owner_key = ?", Long.class, ownerKey);
        return count == null ? 0L : count;
    }

    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM notes WHERE id = ?)",
            Boolean.class,
            id
        );
        return Boolean.TRUE.equals(exists);
    }

    public List<RelatedNoteDto> findRelated(Long id, String viewerKey) {
        NoteDto source = findAccessible(id, viewerKey);
        List<NoteCandidate> candidates = jdbcTemplate.query(
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
                  n.visibility,
                  n.status,
                  n.category,
                  n.updated_at
                FROM notes n
                LEFT JOIN users u ON u.login = n.owner_key
                WHERE n.id <> ?
                  AND (
                    (? IS NOT NULL AND n.owner_key = ?)
                    OR (n.visibility = 'PUBLIC' AND n.status = 'PUBLISHED')
                  )
                ORDER BY n.updated_at DESC, n.id DESC
                LIMIT 40
                """,
            (resultSet, rowNum) -> mapCandidate(resultSet),
            id,
            viewerKey,
            viewerKey
        );

        Set<String> sourceTokens = extractTokens(source.title() + "\n" + source.contentMd());
        return candidates.stream()
            .sorted(
                Comparator.comparingInt((NoteCandidate candidate) -> relatedScore(source, sourceTokens, candidate)).reversed()
                    .thenComparing(NoteCandidate::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(NoteCandidate::id, Comparator.reverseOrder())
            )
            .limit(4)
            .map(this::toRelatedDto)
            .toList();
    }

    public void recordView(Long id, String viewerKey) {
        if (viewerKey == null || viewerKey.isBlank()) {
            return;
        }
        int updated = jdbcTemplate.update(
            """
                UPDATE note_view_history
                SET viewed_at = CURRENT_TIMESTAMP
                WHERE note_id = ? AND user_key = ?
                """,
            id,
            viewerKey
        );
        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO note_view_history (note_id, user_key, viewed_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    """,
                id,
                viewerKey
            );
        }
    }

    public PageResponse<RelatedNoteDto> listViewHistory(String viewerKey, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM note_view_history h
                JOIN notes n ON n.id = h.note_id
                WHERE h.user_key = ?
                """,
            Long.class,
            viewerKey
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
                  h.viewed_at,
                  COUNT(f.id) AS favorite_count,
                  MAX(CASE WHEN uf.user_key IS NOT NULL THEN 1 ELSE 0 END) AS favorited
                FROM note_view_history h
                JOIN notes n ON n.id = h.note_id
                LEFT JOIN users u ON u.login = n.owner_key
                LEFT JOIN favorites f ON f.note_id = n.id AND f.resource_id IS NULL
                LEFT JOIN favorites uf
                  ON uf.note_id = n.id
                 AND uf.resource_id IS NULL
                 AND uf.user_key = ?
                WHERE h.user_key = ?
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
                  h.viewed_at
                ORDER BY h.viewed_at DESC, n.id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> mapFeedDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getTimestamp("viewed_at"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                resultSet.getLong("favorite_count"),
                resultSet.getInt("favorited") > 0
            ),
            viewerKey,
            viewerKey,
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    private Optional<NoteDto> findOptional(Long id, String ownerKey) {
        List<NoteDto> results = jdbcTemplate.query(
            """
                SELECT
                  n.id,
                  n.title,
                  n.content_md,
                  n.visibility,
                  n.status,
                  n.category,
                  n.created_at,
                  n.updated_at,
                  n.is_official,
                  n.is_pinned,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key) AS owner_name,
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END AS owner_avatar_url
                FROM notes n
                LEFT JOIN users u ON u.login = n.owner_key
                WHERE n.id = ? AND n.owner_key = ?
                """,
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at")),
                resultSet.getBoolean("is_official"),
                resultSet.getBoolean("is_pinned")
            ),
            id,
            ownerKey
        );
        return results.stream().findFirst();
    }

    private Optional<NoteDto> findPublicPublished(Long id) {
        List<NoteDto> results = jdbcTemplate.query(
            """
                SELECT
                  n.id,
                  n.title,
                  n.content_md,
                  n.visibility,
                  n.status,
                  n.category,
                  n.created_at,
                  n.updated_at,
                  n.is_official,
                  n.is_pinned,
                  n.owner_key,
                  COALESCE(u.name, n.owner_key) AS owner_name,
                  CASE
                    WHEN u.avatar_file_id IS NULL THEN NULL
                    ELSE '/api/files/' || u.avatar_file_id::text
                  END AS owner_avatar_url
                FROM notes n
                LEFT JOIN users u ON u.login = n.owner_key
                WHERE n.id = ? AND n.visibility = 'PUBLIC' AND n.status = 'PUBLISHED'
                """,
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status"),
                resultSet.getString("category"),
                resultSet.getString("owner_key"),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_avatar_url"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at")),
                resultSet.getBoolean("is_official"),
                resultSet.getBoolean("is_pinned")
            ),
            id
        );
        return results.stream().findFirst();
    }

    private NoteDto mapDto(
        Long id,
        String title,
        String contentMd,
        String visibility,
        String status,
        String category,
        String ownerKey,
        String ownerName,
        String ownerAvatarUrl,
        Instant createdAt,
        Instant updatedAt,
        boolean isOfficial,
        boolean isPinned
    ) {
        return new NoteDto(
            id,
            title,
            contentMd,
            visibility,
            status,
            category,
            ownerKey,
            ownerName,
            ownerAvatarUrl,
            createdAt,
            updatedAt,
            isOfficial,
            isPinned
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private NoteCandidate mapCandidate(ResultSet resultSet) throws SQLException {
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        return new NoteCandidate(
            resultSet.getLong("id"),
            resultSet.getString("title"),
            resultSet.getString("content_md"),
            resultSet.getString("owner_key"),
            resultSet.getString("owner_name"),
            resultSet.getString("owner_avatar_url"),
            resultSet.getString("visibility"),
            resultSet.getString("status"),
            resultSet.getString("category"),
            updatedAt == null ? null : updatedAt.toInstant()
        );
    }

    private RelatedNoteDto toRelatedDto(NoteCandidate candidate) {
        return new RelatedNoteDto(
            candidate.id(),
            candidate.title(),
            extractSummary(candidate.contentMd()),
            candidate.updatedAt(),
            candidate.status(),
            candidate.category(),
            candidate.category() == null ? List.of() : List.of(candidate.category()),
            candidate.ownerKey(),
            candidate.ownerName(),
            candidate.ownerAvatarUrl(),
            0L,
            false
        );
    }

    private RelatedNoteDto mapFeedDto(
        Long id,
        String title,
        String contentMd,
        Timestamp activityAt,
        String status,
        String category,
        String ownerKey,
        String ownerName,
        String ownerAvatarUrl,
        long favorites,
        boolean favorited
    ) {
        return new RelatedNoteDto(
            id,
            title,
            extractSummary(contentMd),
            activityAt == null ? null : activityAt.toInstant(),
            status,
            category,
            category == null ? List.of() : List.of(category),
            ownerKey,
            ownerName,
            ownerAvatarUrl,
            favorites,
            favorited
        );
    }

    private int relatedScore(NoteDto source, Set<String> sourceTokens, NoteCandidate candidate) {
        int score = 0;
        if (source.title().equalsIgnoreCase(candidate.title())) {
            score += 4;
        }
        if (source.visibility() != null && source.visibility().equalsIgnoreCase(candidate.visibility())) {
            score += 1;
        }
        if (source.status() != null && source.status().equalsIgnoreCase(candidate.status())) {
            score += 1;
        }
        Set<String> candidateTokens = extractTokens(candidate.title() + "\n" + candidate.contentMd());
        for (String token : candidateTokens) {
            if (sourceTokens.contains(token)) {
                score += 2;
            }
        }
        return score;
    }

    private Set<String> extractTokens(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Set.of();
        }
        String normalized = rawText
            .replace('#', ' ')
            .replace('`', ' ')
            .replace('*', ' ')
            .trim()
            .toLowerCase();
        Set<String> tokens = new HashSet<>();
        for (String part : TOKEN_SPLIT.split(normalized)) {
            String token = part.trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String extractSummary(String contentMd) {
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

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultStatus(String status) {
        String normalized = normalize(status);
        return normalized == null ? "DRAFT" : normalized;
    }

    private String normalize(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim();
    }

    private String normalizeForUpdate(String status, String existingStatus) {
        String normalized = normalize(status);
        return normalized == null ? existingStatus : normalized;
    }

    private record NoteCandidate(
        Long id,
        String title,
        String contentMd,
        String ownerKey,
        String ownerName,
        String ownerAvatarUrl,
        String visibility,
        String status,
        String category,
        Instant updatedAt
    ) {}
}
