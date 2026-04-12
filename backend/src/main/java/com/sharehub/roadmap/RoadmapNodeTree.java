package com.sharehub.roadmap;

import java.util.List;

public record RoadmapNodeTree(
    Long id,
    Long parentId,
    String title,
    String description,
    Integer orderNo,
    Long resourceId,
    Long noteId,
    List<RoadmapNodeAttachmentDto> attachments,
    List<RoadmapNodeTree> children
) {}
