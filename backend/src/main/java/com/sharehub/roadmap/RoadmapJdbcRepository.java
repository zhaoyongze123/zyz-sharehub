package com.sharehub.roadmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
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
    String countSql =
        normalizedStatus == null
            ? """
                SELECT COUNT(*)
                FROM roadmaps
                WHERE owner_key = ?
                """
            : """
                SELECT COUNT(*)
                FROM roadmaps
                WHERE owner_key = ?
                  AND status = ?
                """;
    Long total =
        normalizedStatus == null
            ? jdbc.queryForObject(countSql, Long.class, ownerKey)
            : jdbc.queryForObject(countSql, Long.class, ownerKey, normalizedStatus);
    int offset = (safePage - 1) * safePageSize;
    String listSql =
        normalizedStatus == null
            ? """
                SELECT r.id,
                       r.title,
                       r.description,
                       r.visibility,
                       r.status,
                       COUNT(n.id) AS node_count,
                       np.completed_node_count AS structured_completed_node_count,
                       p.payload AS progress_payload,
                       CAST(NULL AS VARCHAR(32)) AS enrollment_status,
                       CAST(NULL AS TIMESTAMP) AS started_at,
                       CAST(NULL AS TIMESTAMP) AS completed_at
                FROM roadmaps r
                LEFT JOIN roadmap_nodes n ON n.roadmap_id = r.id
                LEFT JOIN (
                  SELECT roadmap_id, user_key, COUNT(*) AS completed_node_count
                  FROM roadmap_node_progress
                  WHERE status = 'COMPLETED'
                  GROUP BY roadmap_id, user_key
                ) np ON np.roadmap_id = r.id AND np.user_key = ?
                LEFT JOIN roadmap_progress p
                  ON p.roadmap_id = r.id AND p.user_key = ?
                WHERE r.owner_key = ?
                GROUP BY r.id, r.title, r.description, r.visibility, r.status, np.completed_node_count, p.payload
                ORDER BY r.id DESC
                LIMIT ? OFFSET ?
                """
            : """
                SELECT r.id,
                       r.title,
                       r.description,
                       r.visibility,
                       r.status,
                       COUNT(n.id) AS node_count,
                       np.completed_node_count AS structured_completed_node_count,
                       p.payload AS progress_payload,
                       CAST(NULL AS VARCHAR(32)) AS enrollment_status,
                       CAST(NULL AS TIMESTAMP) AS started_at,
                       CAST(NULL AS TIMESTAMP) AS completed_at
                FROM roadmaps r
                LEFT JOIN roadmap_nodes n ON n.roadmap_id = r.id
                LEFT JOIN (
                  SELECT roadmap_id, user_key, COUNT(*) AS completed_node_count
                  FROM roadmap_node_progress
                  WHERE status = 'COMPLETED'
                  GROUP BY roadmap_id, user_key
                ) np ON np.roadmap_id = r.id AND np.user_key = ?
                LEFT JOIN roadmap_progress p
                  ON p.roadmap_id = r.id AND p.user_key = ?
                WHERE r.owner_key = ?
                  AND r.status = ?
                GROUP BY r.id, r.title, r.description, r.visibility, r.status, np.completed_node_count, p.payload
                ORDER BY r.id DESC
                LIMIT ? OFFSET ?
                """;
    List<RoadmapWorkbenchDto> items =
        normalizedStatus == null
            ? jdbc.query(
                listSql,
                (rs, rowNum) -> mapWorkbench(rs),
                ownerKey,
                ownerKey,
                ownerKey,
                safePageSize,
                offset)
            : jdbc.query(
                listSql,
                (rs, rowNum) -> mapWorkbench(rs),
                ownerKey,
                ownerKey,
                ownerKey,
                normalizedStatus,
                safePageSize,
                offset);
    return PageResponse.of(items, safePage, safePageSize, total == null ? 0L : total);
  }

  public PageResponse<RoadmapWorkbenchDto> listWorkbenchByEnrollment(String userKey, String status, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, pageSize);
    String normalizedStatus = normalize(status);
    String countSql =
        normalizedStatus == null
            ? """
                SELECT COUNT(*)
                FROM (
                  SELECT e.roadmap_id
                  FROM roadmap_enrollments e
                  WHERE e.user_key = ?
                  UNION
                  SELECT p.roadmap_id
                  FROM roadmap_progress p
                  WHERE p.user_key = ?
                    AND NOT EXISTS (
                      SELECT 1
                      FROM roadmap_enrollments e
                      WHERE e.roadmap_id = p.roadmap_id
                        AND e.user_key = p.user_key
                    )
                ) t
                """
            : """
                SELECT COUNT(*)
                FROM (
                  SELECT e.roadmap_id
                  FROM roadmap_enrollments e
                  WHERE e.user_key = ?
                    AND e.status = ?
                  UNION
                  SELECT p.roadmap_id
                  FROM roadmap_progress p
                  WHERE p.user_key = ?
                    AND ? = 'ACTIVE'
                    AND NOT EXISTS (
                      SELECT 1
                      FROM roadmap_enrollments e
                      WHERE e.roadmap_id = p.roadmap_id
                        AND e.user_key = p.user_key
                    )
                ) t
                """;
    Long total =
        normalizedStatus == null
            ? jdbc.queryForObject(countSql, Long.class, userKey, userKey)
            : jdbc.queryForObject(countSql, Long.class, userKey, normalizedStatus, userKey, normalizedStatus);
    int offset = (safePage - 1) * safePageSize;
    String listSql =
        normalizedStatus == null
            ? """
                WITH enrollment_source AS (
                  SELECT e.roadmap_id,
                         e.user_key,
                         e.status AS enrollment_status,
                         e.started_at,
                         e.completed_at,
                         e.updated_at
                  FROM roadmap_enrollments e
                  WHERE e.user_key = ?
                  UNION ALL
                  SELECT p.roadmap_id,
                         p.user_key,
                         'ACTIVE' AS enrollment_status,
                         NULL AS started_at,
                         NULL AS completed_at,
                         p.updated_at
                  FROM roadmap_progress p
                  WHERE p.user_key = ?
                    AND NOT EXISTS (
                      SELECT 1
                      FROM roadmap_enrollments e
                      WHERE e.roadmap_id = p.roadmap_id
                        AND e.user_key = p.user_key
                    )
                )
                SELECT r.id,
                       r.title,
                       r.description,
                       r.visibility,
                       r.status,
                       COUNT(n.id) AS node_count,
                       np.completed_node_count AS structured_completed_node_count,
                       p.payload AS progress_payload,
                       es.enrollment_status,
                       es.started_at,
                       es.completed_at
                FROM enrollment_source es
                JOIN roadmaps r ON r.id = es.roadmap_id
                LEFT JOIN roadmap_nodes n ON n.roadmap_id = r.id
                LEFT JOIN (
                  SELECT roadmap_id, user_key, COUNT(*) AS completed_node_count
                  FROM roadmap_node_progress
                  WHERE status = 'COMPLETED'
                  GROUP BY roadmap_id, user_key
                ) np ON np.roadmap_id = r.id AND np.user_key = es.user_key
                LEFT JOIN roadmap_progress p
                  ON p.roadmap_id = r.id AND p.user_key = es.user_key
                GROUP BY
                  r.id, r.title, r.description, r.visibility, r.status,
                  np.completed_node_count, p.payload, es.enrollment_status, es.started_at, es.completed_at, es.updated_at
                ORDER BY es.updated_at DESC, r.id DESC
                LIMIT ? OFFSET ?
                """
            : """
                WITH enrollment_source AS (
                  SELECT e.roadmap_id,
                         e.user_key,
                         e.status AS enrollment_status,
                         e.started_at,
                         e.completed_at,
                         e.updated_at
                  FROM roadmap_enrollments e
                  WHERE e.user_key = ?
                    AND e.status = ?
                  UNION ALL
                  SELECT p.roadmap_id,
                         p.user_key,
                         'ACTIVE' AS enrollment_status,
                         NULL AS started_at,
                         NULL AS completed_at,
                         p.updated_at
                  FROM roadmap_progress p
                  WHERE p.user_key = ?
                    AND ? = 'ACTIVE'
                    AND NOT EXISTS (
                      SELECT 1
                      FROM roadmap_enrollments e
                      WHERE e.roadmap_id = p.roadmap_id
                        AND e.user_key = p.user_key
                    )
                )
                SELECT r.id,
                       r.title,
                       r.description,
                       r.visibility,
                       r.status,
                       COUNT(n.id) AS node_count,
                       np.completed_node_count AS structured_completed_node_count,
                       p.payload AS progress_payload,
                       es.enrollment_status,
                       es.started_at,
                       es.completed_at
                FROM enrollment_source es
                JOIN roadmaps r ON r.id = es.roadmap_id
                LEFT JOIN roadmap_nodes n ON n.roadmap_id = r.id
                LEFT JOIN (
                  SELECT roadmap_id, user_key, COUNT(*) AS completed_node_count
                  FROM roadmap_node_progress
                  WHERE status = 'COMPLETED'
                  GROUP BY roadmap_id, user_key
                ) np ON np.roadmap_id = r.id AND np.user_key = es.user_key
                LEFT JOIN roadmap_progress p
                  ON p.roadmap_id = r.id AND p.user_key = es.user_key
                GROUP BY
                  r.id, r.title, r.description, r.visibility, r.status,
                  np.completed_node_count, p.payload, es.enrollment_status, es.started_at, es.completed_at, es.updated_at
                ORDER BY es.updated_at DESC, r.id DESC
                LIMIT ? OFFSET ?
                """;
    List<RoadmapWorkbenchDto> items =
        normalizedStatus == null
            ? jdbc.query(listSql, (rs, rowNum) -> mapWorkbench(rs), userKey, userKey, safePageSize, offset)
            : jdbc.query(
                listSql,
                (rs, rowNum) -> mapWorkbench(rs),
                userKey,
                normalizedStatus,
                userKey,
                normalizedStatus,
                safePageSize,
                offset
            );
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
        "SELECT id, parent_id, title, description, order_no, resource_id, note_id FROM roadmap_nodes WHERE roadmap_id = ? ORDER BY order_no NULLS LAST, id",
        (rs, rowNum) ->
            new RoadmapNodeDto(
                rs.getLong("id"),
                nullableLong(rs, "parent_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getObject("order_no") == null ? null : rs.getInt("order_no"),
                nullableLong(rs, "resource_id"),
                nullableLong(rs, "note_id"),
                List.of()),
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
                      "INSERT INTO roadmap_nodes (roadmap_id, parent_id, title, description, order_no, resource_id, note_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
              statement.setLong(1, roadmapId);
              bindLong(statement, 2, req.parentId());
              statement.setString(3, req.title());
              statement.setString(4, req.description());
              if (req.orderNo() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
              } else {
                statement.setInt(5, req.orderNo());
              }
              bindLong(statement, 6, req.resourceId());
              bindLong(statement, 7, req.noteId());
              return statement;
            });
    return findNodes(roadmapId);
  }

  public boolean existsOwnedNode(Long roadmapId, Long nodeId, String ownerKey) {
    Integer count =
        jdbc.queryForObject(
            """
                SELECT COUNT(*)
                FROM roadmap_nodes n
                JOIN roadmaps r ON r.id = n.roadmap_id
                WHERE n.id = ? AND n.roadmap_id = ? AND r.owner_key = ?
                """,
            Integer.class,
            nodeId,
            roadmapId,
            ownerKey);
    return count != null && count > 0;
  }

  @Transactional
  public Map<String, Object> saveProgress(String ownerKey, Long roadmapId, Map<String, Object> payload) {
    requireRoadmap(roadmapId, ownerKey);
    Map<String, Object> normalizedPayload = normalizeProgressPayload(roadmapId, payload);
    syncNodeProgress(roadmapId, ownerKey, extractCompletedNodeIds(normalizedPayload));
    String json = serialize(normalizedPayload);
    Instant now = Instant.now();
    int updated =
        jdbc.update(
            "UPDATE roadmap_progress SET payload = CAST(? AS jsonb), updated_at = ? WHERE roadmap_id = ? AND user_key = ?",
            json,
            Timestamp.from(now),
            roadmapId,
            ownerKey);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO roadmap_progress (roadmap_id, user_key, payload, updated_at) VALUES (?, ?, CAST(? AS jsonb), ?)",
          roadmapId,
          ownerKey,
          json,
          Timestamp.from(now));
    }
    return normalizedPayload;
  }

  public Map<String, Object> findProgress(Long roadmapId, String ownerKey) {
    Map<String, Object> progress =
        jdbc.query(
            "SELECT payload FROM roadmap_progress WHERE roadmap_id = ? AND user_key = ?",
            (ResultSet rs) -> rs.next() ? deserialize(rs.getString("payload")) : null,
            roadmapId,
            ownerKey);
    if (progress != null) {
      return progress;
    }
    List<Long> completedNodeIds = findCompletedNodeIds(roadmapId, ownerKey);
    if (completedNodeIds.isEmpty()) {
      return null;
    }
    return buildProgressPayload(roadmapId, completedNodeIds, null);
  }

  public long countByOwner(String ownerKey) {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps WHERE owner_key = ?", Long.class, ownerKey);
    return count == null ? 0L : count;
  }

  public void requireRoadmapExists(Long roadmapId) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM roadmaps WHERE id = ?", Integer.class, roadmapId);
    if (count == null || count == 0) {
      throw new NotFoundException("ROADMAP_NOT_FOUND");
    }
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
    Integer structuredCompletedNodeCount = nullableInteger(resultSet, "structured_completed_node_count");
    int completedNodeCount = structuredCompletedNodeCount == null
        ? extractCompletedNodeCount(progress)
        : structuredCompletedNodeCount;
    int percent = extractPercent(progress, nodeCount, completedNodeCount);
    return new RoadmapWorkbenchDto(
        resultSet.getLong("id"),
        resultSet.getString("title"),
        resultSet.getString("description"),
        resultSet.getString("visibility"),
        resultSet.getString("status"),
        nodeCount,
        completedNodeCount,
        percent,
        resultSet.getString("enrollment_status"),
        toInstant(resultSet.getTimestamp("started_at")),
        toInstant(resultSet.getTimestamp("completed_at")));
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private int extractCompletedNodeCount(Map<String, Object> progress) {
    return extractCompletedNodeIds(progress).size();
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
    return value.trim();
  }

  private Integer nullableInteger(ResultSet resultSet, String column) throws java.sql.SQLException {
    Object value = resultSet.getObject(column);
    return value == null ? null : resultSet.getInt(column);
  }

  private Map<String, Object> normalizeProgressPayload(Long roadmapId, Map<String, Object> payload) {
    List<Long> completedNodeIds = normalizeCompletedNodeIds(roadmapId, extractCompletedNodeIds(payload));
    Integer requestedPercent = extractRequestedPercent(payload);
    return buildProgressPayload(roadmapId, completedNodeIds, requestedPercent);
  }

  private Map<String, Object> buildProgressPayload(Long roadmapId, List<Long> completedNodeIds, Integer requestedPercent) {
    Map<String, Object> normalizedPayload = new HashMap<>();
    normalizedPayload.put("completedNodeIds", completedNodeIds);
    normalizedPayload.put("percent", requestedPercent == null ? calculatePercent(roadmapId, completedNodeIds.size()) : requestedPercent);
    return normalizedPayload;
  }

  private Integer extractRequestedPercent(Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }
    Object value = payload.get("percent");
    if (value instanceof Number number) {
      return number.intValue();
    }
    return null;
  }

  private List<Long> extractCompletedNodeIds(Map<String, Object> progress) {
    if (progress == null) {
      return List.of();
    }
    Object value = progress.get("completedNodeIds");
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .map(this::toLong)
        .filter(id -> id != null)
        .distinct()
        .toList();
  }

  private Long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text) {
      try {
        return Long.valueOf(text.trim());
      } catch (NumberFormatException exception) {
        return null;
      }
    }
    return null;
  }

  private List<Long> normalizeCompletedNodeIds(Long roadmapId, List<Long> completedNodeIds) {
    if (completedNodeIds.isEmpty()) {
      return List.of();
    }
    List<Long> validNodeIds = findNodeIds(roadmapId);
    if (validNodeIds.isEmpty()) {
      return List.of();
    }
    return completedNodeIds.stream()
        .filter(validNodeIds::contains)
        .distinct()
        .toList();
  }

  private List<Long> findNodeIds(Long roadmapId) {
    return jdbc.queryForList(
        "SELECT id FROM roadmap_nodes WHERE roadmap_id = ? ORDER BY order_no NULLS LAST, id",
        Long.class,
        roadmapId);
  }

  private int calculatePercent(Long roadmapId, int completedNodeCount) {
    Integer nodeCount = jdbc.queryForObject("SELECT COUNT(*) FROM roadmap_nodes WHERE roadmap_id = ?", Integer.class, roadmapId);
    if (nodeCount == null || nodeCount <= 0) {
      return 0;
    }
    return (int) Math.round((completedNodeCount * 100.0) / nodeCount);
  }

  private List<Long> findCompletedNodeIds(Long roadmapId, String userKey) {
    return jdbc.queryForList(
        """
            SELECT node_id
            FROM roadmap_node_progress
            WHERE roadmap_id = ? AND user_key = ? AND status = 'COMPLETED'
            ORDER BY node_id
            """,
        Long.class,
        roadmapId,
        userKey);
  }

  @Transactional
  private void syncNodeProgress(Long roadmapId, String userKey, List<Long> completedNodeIds) {
    Instant now = Instant.now();
    upsertCompletedNodeProgress(roadmapId, userKey, completedNodeIds, now);
    resetIncompleteNodeProgress(roadmapId, userKey, completedNodeIds, now);
  }

  private void upsertCompletedNodeProgress(Long roadmapId, String userKey, List<Long> completedNodeIds, Instant now) {
    if (completedNodeIds.isEmpty()) {
      return;
    }
    String sql =
        """
            INSERT INTO roadmap_node_progress
              (roadmap_id, node_id, user_key, status, completed_at, created_at, updated_at)
            VALUES (?, ?, ?, 'COMPLETED', ?, ?, ?)
            ON CONFLICT (node_id, user_key)
            DO UPDATE SET
              roadmap_id = EXCLUDED.roadmap_id,
              status = EXCLUDED.status,
              completed_at = EXCLUDED.completed_at,
              updated_at = EXCLUDED.updated_at
            """;
    Timestamp timestamp = Timestamp.from(now);
    jdbc.batchUpdate(
        sql,
        completedNodeIds,
        completedNodeIds.size(),
        (statement, nodeId) -> {
          statement.setLong(1, roadmapId);
          statement.setLong(2, nodeId);
          statement.setString(3, userKey);
          statement.setTimestamp(4, timestamp);
          statement.setTimestamp(5, timestamp);
          statement.setTimestamp(6, timestamp);
        });
  }

  private void resetIncompleteNodeProgress(Long roadmapId, String userKey, List<Long> completedNodeIds, Instant now) {
    Timestamp timestamp = Timestamp.from(now);
    if (completedNodeIds.isEmpty()) {
      jdbc.update(
          """
              UPDATE roadmap_node_progress
              SET status = 'NOT_STARTED', completed_at = NULL, updated_at = ?
              WHERE roadmap_id = ? AND user_key = ?
              """,
          timestamp,
          roadmapId,
          userKey);
      return;
    }
    String placeholders = String.join(", ", completedNodeIds.stream().map(nodeId -> "?").toList());
    String sql =
        """
            UPDATE roadmap_node_progress
            SET status = 'NOT_STARTED', completed_at = NULL, updated_at = ?
            WHERE roadmap_id = ? AND user_key = ? AND node_id NOT IN (%s)
            """
            .formatted(placeholders);
    Object[] params = new Object[completedNodeIds.size() + 3];
    params[0] = timestamp;
    params[1] = roadmapId;
    params[2] = userKey;
    for (int index = 0; index < completedNodeIds.size(); index++) {
      params[index + 3] = completedNodeIds.get(index);
    }
    jdbc.update(sql, params);
  }
}
