package com.sharehub.note;

import jakarta.validation.constraints.NotBlank;

public record NoteDto(
    Long id,
    @NotBlank String title,
    @NotBlank String contentMd,
    String visibility,
    String status,
    String category,
    String ownerKey,
    String ownerName,
    String ownerAvatarUrl
) {}
