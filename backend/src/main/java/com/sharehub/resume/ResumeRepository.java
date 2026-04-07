package com.sharehub.resume;

import com.sharehub.common.NotFoundException;
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
                        INSERT INTO resumes (template_key, status, file_id, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, templateKey);
                statement.setString(2, "GENERATED");
                statement.setObject(3, fileId);
                return statement;
            },
            keyHolder
        );
        return find(keyHolder.getKey().longValue());
    }

    public ResumeDto find(Long id) {
        return findOptional(id).orElseThrow(() -> new NotFoundException("RESUME_NOT_FOUND"));
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
