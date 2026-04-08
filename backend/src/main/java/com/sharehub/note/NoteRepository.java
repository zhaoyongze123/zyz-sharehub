package com.sharehub.note;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {

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
                        INSERT INTO notes (title, content_md, owner_key, visibility, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, note.title());
                statement.setString(2, note.contentMd());
                statement.setString(3, ownerKey);
                statement.setString(4, nullable(note.visibility()));
                statement.setString(5, defaultStatus(note.status()));
                return statement;
            },
            keyHolder
        );
        return findOwned(keyHolder.getKey().longValue(), ownerKey);
    }

    public PageResponse<NoteDto> list(String ownerKey, int page, int pageSize) {
        return listByOwner(ownerKey, null, page, pageSize);
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
                SELECT id, title, content_md, visibility, status
                FROM notes
                WHERE owner_key = ?
                """
        );
        List<Object> listArgs = new ArrayList<>();
        listArgs.add(ownerKey);
        if (normalizedStatus != null) {
            listSql.append(" AND status = ?");
            listArgs.add(normalizedStatus);
        }
        listSql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        listArgs.add(safePageSize);
        listArgs.add(offset);

        List<NoteDto> items = jdbcTemplate.query(
            listSql.toString(),
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status")
            ),
            listArgs.toArray()
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public NoteDto findOwned(Long id, String ownerKey) {
        return findOptional(id, ownerKey).orElseThrow(() -> new NotFoundException("NOTE_NOT_FOUND"));
    }

    public NoteDto upsertOwned(Long id, String ownerKey, NoteDto note) {
        NoteDto existing = findOwned(id, ownerKey);
        jdbcTemplate.update(
            """
                UPDATE notes
                SET title = ?, content_md = ?, visibility = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND owner_key = ?
                """,
            note.title(),
            note.contentMd(),
            nullable(note.visibility()),
            note.status() == null ? existing.status() : note.status(),
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

    public long countByOwner(String ownerKey) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE owner_key = ?", Long.class, ownerKey);
        return count == null ? 0L : count;
    }

    private Optional<NoteDto> findOptional(Long id, String ownerKey) {
        List<NoteDto> results = jdbcTemplate.query(
            "SELECT id, title, content_md, visibility, status FROM notes WHERE id = ? AND owner_key = ?",
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status")
            ),
            id,
            ownerKey
        );
        return results.stream().findFirst();
    }

    private NoteDto mapDto(Long id, String title, String contentMd, String visibility, String status) {
        return new NoteDto(id, title, contentMd, visibility, status);
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultStatus(String status) {
        return status == null || status.isBlank() ? "DRAFT" : status;
    }

    private String normalize(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status;
    }
}
