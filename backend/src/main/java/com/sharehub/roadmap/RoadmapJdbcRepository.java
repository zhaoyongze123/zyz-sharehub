package com.sharehub.roadmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RoadmapJdbcRepository {

  private static final String DEFAULT_USER_KEY = "local-dev-user";

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public RoadmapJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public RoadmapDto save(RoadmapDto dto) {
    KeyHolder holder = new GeneratedKeyHolder();
    jdbc.update(
        (PreparedStatementCreator)
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO roadmaps (title, description, visibility, status) VALUES (?, ?, ?, ?)",
                      new String[] {"id"});
              statement.setString(1, dto.title());
              statement.setString(2, dto.description());
              statement.setString(3, dto.visibility());
              statement.setString(4, dto.status());
              return statement;
            },
        holder);
    return new RoadmapDto(holder.getKey().longValue(), dto.title(), dto.description(), dto.visibility(), dto.status());
  }

  public PageResponse<RoadmapDto> list(int page, int pageSize) {
    long total = Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps", Long.class)).orElse(0L);
    int offset = Math.max(0, page - 1) * pageSize;
    List<RoadmapDto> records =
        jdbc.query(
            "SELECT id, title, description, visibility, status FROM roadmaps ORDER BY id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) ->
                new RoadmapDto(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("visibility"),
                    rs.getString("status")),
            pageSize,
            offset);
    return PageResponse.of(records, page, pageSize, total);
  }

  public Optional<RoadmapDto> findById(Long id) {
    List<RoadmapDto> results =
        jdbc.query(
            "SELECT id, title, description, visibility, status FROM roadmaps WHERE id = ?",
            (rs, rowNum) ->
                new RoadmapDto(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("visibility"),
                    rs.getString("status")),
            id);
    return results.stream().findFirst();
  }

  public List<RoadmapNodeDto> findNodes(Long roadmapId) {
    return jdbc.query(
        "SELECT id, parent_id, title, order_no, resource_id, note_id FROM roadmap_nodes WHERE roadmap_id = ? ORDER BY order_no NULLS LAST, id",
        (rs, rowNum) ->
            new RoadmapNodeDto(
                rs.getLong("id"),
                nullableLong(rs, "parent_id"),
                rs.getString("title"),
                rs.getObject("order_no") == null ? null : rs.getInt("order_no"),
                nullableLong(rs, "resource_id"),
                nullableLong(rs, "note_id")),
        roadmapId);
  }

  @Transactional
  public List<RoadmapNodeDto> addNode(Long roadmapId, RoadmapNodeDto req) {
    requireRoadmap(roadmapId);
    jdbc.update(
        (PreparedStatementCreator)
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO roadmap_nodes (roadmap_id, parent_id, title, order_no, resource_id, note_id) VALUES (?, ?, ?, ?, ?, ?)");
              statement.setLong(1, roadmapId);
              bindLong(statement, 2, req.parentId());
              statement.setString(3, req.title());
              if (req.orderNo() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
              } else {
                statement.setInt(4, req.orderNo());
              }
              bindLong(statement, 5, req.resourceId());
              bindLong(statement, 6, req.noteId());
              return statement;
            });
    return findNodes(roadmapId);
  }

  @Transactional
  public Map<String, Object> saveProgress(Long roadmapId, Map<String, Object> payload) {
    requireRoadmap(roadmapId);
    String json = serialize(payload);
    int updated =
        jdbc.update(
            "UPDATE roadmap_progress SET payload = ?, updated_at = ? WHERE roadmap_id = ? AND user_key = ?",
            json,
            Timestamp.from(Instant.now()),
            roadmapId,
            DEFAULT_USER_KEY);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO roadmap_progress (roadmap_id, user_key, payload, updated_at) VALUES (?, ?, ?, ?)",
          roadmapId,
          DEFAULT_USER_KEY,
          json,
          Timestamp.from(Instant.now()));
    }
    return payload;
  }

  public Map<String, Object> findProgress(Long roadmapId) {
    return jdbc.query(
        "SELECT payload FROM roadmap_progress WHERE roadmap_id = ? AND user_key = ?",
        (ResultSet rs) -> rs.next() ? deserialize(rs.getString("payload")) : null,
        roadmapId,
        DEFAULT_USER_KEY);
  }

  private void requireRoadmap(Long roadmapId) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps WHERE id = ?", Integer.class, roadmapId);
    if (count == null || count == 0) {
      throw new NotFoundException("ROADMAP_NOT_FOUND");
    }
  }

  private void bindLong(PreparedStatement statement, int index, Long value) throws java.sql.SQLException {
    if (value == null) {
      statement.setNull(index, java.sql.Types.BIGINT);
    } else {
      statement.setLong(index, value);
    }
  }

  private Long nullableLong(ResultSet resultSet, String column) throws java.sql.SQLException {
    Object value = resultSet.getObject(column);
    return value == null ? null : resultSet.getLong(column);
  }

  private String serialize(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize progress", exception);
    }
  }

  private Map<String, Object> deserialize(String value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.readValue(value, Map.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize progress", exception);
    }
  }
}
