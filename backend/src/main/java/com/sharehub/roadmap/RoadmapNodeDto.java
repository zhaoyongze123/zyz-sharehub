package com.sharehub.roadmap;

import jakarta.validation.constraints.NotBlank;

public record RoadmapNodeDto(Long id, Long parentId, @NotBlank String title, Integer orderNo, Long resourceId, Long noteId) {}
