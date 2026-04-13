package com.sharehub.roadmap;

import java.time.Instant;

public record RoadmapEnrollmentDto(
    Long id,
    Long roadmapId,
    String userKey,
    String status,
    Instant startedAt,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
