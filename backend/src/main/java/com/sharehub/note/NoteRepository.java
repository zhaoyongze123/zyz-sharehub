package com.sharehub.note;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
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

    public NoteDto save(NoteDto note) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
            (PreparedStatementCreator) connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    """
                        INSERT INTO notes (title, content_md, visibility, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, note.title());
                statement.setString(2, note.contentMd());
                statement.setString(3, nullable(note.visibility()));
                statement.setString(4, defaultStatus(note.status()));
                return statement;
            },
            keyHolder
        );
        return find(keyHolder.getKey().longValue());
    }

    public PageResponse<NoteDto> list(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes", Long.class);
        int offset = (safePage - 1) * safePageSize;
        List<NoteDto> items = jdbcTemplate.query(
            """
                SELECT id, title, content_md, visibility, status
                FROM notes
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """,
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status")
            ),
            safePageSize,
            offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
    }

    public NoteDto find(Long id) {
        return findOptional(id).orElseThrow(() -> new NotFoundException("NOTE_NOT_FOUND"));
    }

    public NoteDto upsert(Long id, NoteDto note) {
        NoteDto existing = find(id);
        jdbcTemplate.update(
            """
                UPDATE notes
                SET title = ?, content_md = ?, visibility = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
            note.title(),
            note.contentMd(),
            nullable(note.visibility()),
            note.status() == null ? existing.status() : note.status(),
            id
        );
        return find(id);
    }

    public void delete(Long id) {
        int affected = jdbcTemplate.update("DELETE FROM notes WHERE id = ?", id);
        if (affected == 0) {
            throw new NotFoundException("NOTE_NOT_FOUND");
        }
    }

    private Optional<NoteDto> findOptional(Long id) {
        List<NoteDto> results = jdbcTemplate.query(
            "SELECT id, title, content_md, visibility, status FROM notes WHERE id = ?",
            (resultSet, rowNum) -> mapDto(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content_md"),
                resultSet.getString("visibility"),
                resultSet.getString("status")
            ),
            id
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
}
