package com.sharehub.roadmap;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RoadmapNodeDto(
    Long id,
    Long parentId,
    @NotBlank String title,
    String description,
    Integer orderNo,
    Long resourceId,
    Long noteId,
    List<RoadmapNodeAttachmentDto> attachments
) {}
