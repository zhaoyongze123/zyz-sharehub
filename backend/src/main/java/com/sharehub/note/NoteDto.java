package com.sharehub.note;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record NoteDto(
    Long id,
    @NotBlank String title,
    @NotBlank String contentMd,
    String visibility,
    String status,
    String category,
    List<String> tags,
    String ownerKey,
    String ownerName,
    String ownerAvatarUrl,
    Instant createdAt,
    Instant updatedAt,
    boolean isOfficial,
    boolean isPinned
) {
    public NoteDto withTags(List<String> nextTags) {
        return new NoteDto(
            id,
            title,
            contentMd,
            visibility,
            status,
            category,
            nextTags,
            ownerKey,
            ownerName,
            ownerAvatarUrl,
            createdAt,
            updatedAt,
            isOfficial,
            isPinned
        );
    }
}
