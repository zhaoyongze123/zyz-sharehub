package com.sharehub.roadmap;

import java.util.List;

public record RoadmapNodeTree(
    Long id,
    Long parentId,
    String title,
    Integer orderNo,
    Long resourceId,
    Long noteId,
    List<RoadmapNodeTree> children
) {}
