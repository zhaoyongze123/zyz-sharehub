package com.sharehub.note;

import java.time.Instant;
import java.util.List;

public record RelatedNoteDto(
    Long id,
    String title,
    String summary,
    Instant updatedAt,
    String status,
    String category,
    List<String> tags,
    String ownerKey,
    String ownerName,
    String ownerAvatarUrl,
    long favorites,
    boolean favorited
) {
}
