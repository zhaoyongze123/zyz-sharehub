package com.sharehub.roadmap;

import java.util.List;
import java.util.Map;

public record RoadmapDetailResponse(
    RoadmapDto roadmap,
    List<RoadmapNodeTree> nodes,
    Map<String, Object> progress,
    RoadmapEnrollmentDto enrollment
) {}
