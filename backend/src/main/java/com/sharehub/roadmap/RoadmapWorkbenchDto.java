package com.sharehub.roadmap;

public record RoadmapWorkbenchDto(
    Long id,
    String title,
    String description,
    String visibility,
    String status,
    int nodeCount,
    int completedNodeCount,
    int progressPercent
) {
}
