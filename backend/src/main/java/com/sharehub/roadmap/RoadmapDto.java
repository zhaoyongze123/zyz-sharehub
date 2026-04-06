package com.sharehub.roadmap;

import jakarta.validation.constraints.NotBlank;

public record RoadmapDto(Long id, @NotBlank String title, String description, String visibility, String status) {}
