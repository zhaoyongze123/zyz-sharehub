package com.sharehub.roadmap;

import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.NotFoundException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RoadmapEnrollmentRepository {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final JdbcTemplate jdbcTemplate;
    private final UserProfileRepository userProfileRepository;

    public RoadmapEnrollmentRepository(JdbcTemplate jdbcTemplate, UserProfileRepository userProfileRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<RoadmapEnrollmentDto> findByRoadmapIdAndUserKey(Long roadmapId, String userKey) {
        List<RoadmapEnrollmentDto> results = jdbcTemplate.query(
            """
                SELECT id, roadmap_id, user_key, status, started_at, completed_at, created_at, updated_at
                FROM roadmap_enrollments
                WHERE roadmap_id = ? AND user_key = ?
                """,
            (resultSet, rowNum) -> mapEnrollment(resultSet),
            roadmapId,
            userKey
        );
        return results.stream().findFirst();
    }

    @Transactional
    public RoadmapEnrollmentDto createOrActivate(Long roadmapId, String userKey) {
        Optional<RoadmapEnrollmentDto> existing = findByRoadmapIdAndUserKey(roadmapId, userKey);
        Instant now = Instant.now();
        Long userId = resolveUserId(userKey);
        if (existing.isEmpty()) {
            jdbcTemplate.update(
                """
                    INSERT INTO roadmap_enrollments
                    (roadmap_id, user_key, user_id, status, started_at, completed_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                roadmapId,
                userKey,
                userId,
                STATUS_ACTIVE,
                Timestamp.from(now),
                null,
                Timestamp.from(now),
                Timestamp.from(now)
            );
            return findRequired(roadmapId, userKey);
        }

        RoadmapEnrollmentDto enrollment = existing.get();
        if (STATUS_ACTIVE.equals(enrollment.status())) {
            return enrollment;
        }
        if (STATUS_COMPLETED.equals(enrollment.status())) {
            return enrollment;
        }

        jdbcTemplate.update(
            """
                UPDATE roadmap_enrollments
                SET status = ?, started_at = COALESCE(started_at, ?), completed_at = NULL, updated_at = ?
                WHERE roadmap_id = ? AND user_key = ?
                """,
            STATUS_ACTIVE,
            Timestamp.from(now),
            Timestamp.from(now),
            roadmapId,
            userKey
        );
        return findRequired(roadmapId, userKey);
    }

    @Transactional
    public RoadmapEnrollmentDto pause(Long roadmapId, String userKey) {
        requireExists(roadmapId, userKey);
        jdbcTemplate.update(
            """
                UPDATE roadmap_enrollments
                SET status = ?, updated_at = ?
                WHERE roadmap_id = ? AND user_key = ?
                """,
            STATUS_PAUSED,
            Timestamp.from(Instant.now()),
            roadmapId,
            userKey
        );
        return findRequired(roadmapId, userKey);
    }

    @Transactional
    public RoadmapEnrollmentDto resume(Long roadmapId, String userKey) {
        requireExists(roadmapId, userKey);
        Instant now = Instant.now();
        jdbcTemplate.update(
            """
                UPDATE roadmap_enrollments
                SET status = ?, started_at = COALESCE(started_at, ?), completed_at = NULL, updated_at = ?
                WHERE roadmap_id = ? AND user_key = ?
                """,
            STATUS_ACTIVE,
            Timestamp.from(now),
            Timestamp.from(now),
            roadmapId,
            userKey
        );
        return findRequired(roadmapId, userKey);
    }

    @Transactional
    public RoadmapEnrollmentDto complete(Long roadmapId, String userKey) {
        requireExists(roadmapId, userKey);
        Instant now = Instant.now();
        jdbcTemplate.update(
            """
                UPDATE roadmap_enrollments
                SET status = ?, completed_at = ?, updated_at = ?
                WHERE roadmap_id = ? AND user_key = ?
                """,
            STATUS_COMPLETED,
            Timestamp.from(now),
            Timestamp.from(now),
            roadmapId,
            userKey
        );
        return findRequired(roadmapId, userKey);
    }

    private RoadmapEnrollmentDto findRequired(Long roadmapId, String userKey) {
        return findByRoadmapIdAndUserKey(roadmapId, userKey)
            .orElseThrow(() -> new NotFoundException("ROADMAP_ENROLLMENT_NOT_FOUND"));
    }

    private void requireExists(Long roadmapId, String userKey) {
        if (findByRoadmapIdAndUserKey(roadmapId, userKey).isEmpty()) {
            throw new NotFoundException("ROADMAP_ENROLLMENT_NOT_FOUND");
        }
    }

    private RoadmapEnrollmentDto mapEnrollment(ResultSet resultSet) throws java.sql.SQLException {
        return new RoadmapEnrollmentDto(
            resultSet.getLong("id"),
            resultSet.getLong("roadmap_id"),
            resultSet.getString("user_key"),
            resultSet.getString("status"),
            toInstant(resultSet.getTimestamp("started_at")),
            toInstant(resultSet.getTimestamp("completed_at")),
            toInstant(resultSet.getTimestamp("created_at")),
            toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long resolveUserId(String login) {
        return userProfileRepository.findIdOptionalByLogin(login).orElse(null);
    }
}
