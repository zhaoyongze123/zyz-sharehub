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

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public RoadmapJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public RoadmapDto save(String ownerKey, RoadmapDto dto) {
    KeyHolder holder = new GeneratedKeyHolder();
    jdbc.update(
        (PreparedStatementCreator)
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO roadmaps (title, description, owner_key, visibility, status) VALUES (?, ?, ?, ?, ?)",
                      new String[] {"id"});
              statement.setString(1, dto.title());
              statement.setString(2, dto.description());
              statement.setString(3, ownerKey);
              statement.setString(4, dto.visibility());
              statement.setString(5, dto.status());
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

  public PageResponse<RoadmapDto> listByOwner(String ownerKey, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, pageSize);
    long total =
        Optional.ofNullable(
                jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps WHERE owner_key = ?", Long.class, ownerKey))
            .orElse(0L);
    int offset = (safePage - 1) * safePageSize;
    List<RoadmapDto> records =
        jdbc.query(
            """
                SELECT id, title, description, visibility, status
                FROM roadmaps
                WHERE owner_key = ?
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """,
            (rs, rowNum) ->
                new RoadmapDto(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("visibility"),
                    rs.getString("status")),
            ownerKey,
            safePageSize,
            offset);
    return PageResponse.of(records, safePage, safePageSize, total);
  }

  public PageResponse<RoadmapWorkbenchDto> listWorkbenchByOwner(String ownerKey, String status, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, pageSize);
    String normalizedStatus = normalize(status);
    Long total =
        jdbc.queryForObject(
            """
                SELECT COUNT(*)
                FROM roadmaps
                WHERE owner_key = ?
                  AND (? IS NULL OR status = ?)
                """,
            Long.class,
            ownerKey,
            normalizedStatus,
            normalizedStatus);
    int offset = (safePage - 1) * safePageSize;
    List<RoadmapWorkbenchDto> items =
        jdbc.query(
            """
                SELECT r.id,
                       r.title,
                       r.description,
                       r.visibility,
                       r.status,
                       COUNT(n.id) AS node_count,
                       p.payload AS progress_payload
                FROM roadmaps r
                LEFT JOIN roadmap_nodes n ON n.roadmap_id = r.id
                LEFT JOIN roadmap_progress p
                  ON p.roadmap_id = r.id AND p.user_key = ?
                WHERE r.owner_key = ?
                  AND (? IS NULL OR r.status = ?)
                GROUP BY r.id, r.title, r.description, r.visibility, r.status, p.payload
                ORDER BY r.id DESC
                LIMIT ? OFFSET ?
                """,
            (rs, rowNum) -> mapWorkbench(rs),
            ownerKey,
            ownerKey,
            normalizedStatus,
            normalizedStatus,
            safePageSize,
            offset);
    return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
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
  public List<RoadmapNodeDto> addNode(String ownerKey, Long roadmapId, RoadmapNodeDto req) {
    requireRoadmap(roadmapId, ownerKey);
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
  public Map<String, Object> saveProgress(String ownerKey, Long roadmapId, Map<String, Object> payload) {
    requireRoadmap(roadmapId, ownerKey);
    String json = serialize(payload);
    int updated =
        jdbc.update(
            "UPDATE roadmap_progress SET payload = ?, updated_at = ? WHERE roadmap_id = ? AND user_key = ?",
            json,
            Timestamp.from(Instant.now()),
            roadmapId,
            ownerKey);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO roadmap_progress (roadmap_id, user_key, payload, updated_at) VALUES (?, ?, ?, ?)",
          roadmapId,
          ownerKey,
          json,
          Timestamp.from(Instant.now()));
    }
    return payload;
  }

  public Map<String, Object> findProgress(Long roadmapId, String ownerKey) {
    return jdbc.query(
        "SELECT payload FROM roadmap_progress WHERE roadmap_id = ? AND user_key = ?",
        (ResultSet rs) -> rs.next() ? deserialize(rs.getString("payload")) : null,
        roadmapId,
        ownerKey);
  }

  public long countByOwner(String ownerKey) {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps WHERE owner_key = ?", Long.class, ownerKey);
    return count == null ? 0L : count;
  }

  private void requireRoadmap(Long roadmapId, String ownerKey) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM roadmaps WHERE id = ? AND owner_key = ?",
            Integer.class,
            roadmapId,
            ownerKey);
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

  private RoadmapWorkbenchDto mapWorkbench(ResultSet resultSet) throws java.sql.SQLException {
    Map<String, Object> progress = deserialize(resultSet.getString("progress_payload"));
    int nodeCount = resultSet.getInt("node_count");
    int completedNodeCount = extractCompletedNodeCount(progress);
    int percent = extractPercent(progress, nodeCount, completedNodeCount);
    return new RoadmapWorkbenchDto(
        resultSet.getLong("id"),
        resultSet.getString("title"),
        resultSet.getString("description"),
        resultSet.getString("visibility"),
        resultSet.getString("status"),
        nodeCount,
        completedNodeCount,
        percent);
  }

  private int extractCompletedNodeCount(Map<String, Object> progress) {
    if (progress == null) {
      return 0;
    }
    Object value = progress.get("completedNodeIds");
    if (value instanceof List<?> list) {
      return list.size();
    }
    return 0;
  }

  private int extractPercent(Map<String, Object> progress, int nodeCount, int completedNodeCount) {
    if (progress != null && progress.get("percent") instanceof Number number) {
      return number.intValue();
    }
    if (nodeCount <= 0) {
      return 0;
    }
    return (int) Math.round((completedNodeCount * 100.0) / nodeCount);
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
