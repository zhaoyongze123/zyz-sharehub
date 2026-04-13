package com.sharehub.note;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record NoteDto(
    Long id,
    @NotBlank String title,
    @NotBlank String contentMd,
    String visibility,
    String status,
    String category,
    String ownerKey,
    String ownerName,
    String ownerAvatarUrl,
    Instant createdAt,
    Instant updatedAt,
    boolean isOfficial,
    boolean isPinned
) {}
