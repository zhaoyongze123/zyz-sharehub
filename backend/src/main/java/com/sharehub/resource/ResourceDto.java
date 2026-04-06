package com.sharehub.resource;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ResourceDto(
    Long id,
    @NotBlank String title,
    String type,
    String summary,
    List<String> tags,
    String externalUrl,
    String objectKey,
    String visibility,
    String status
) {}
