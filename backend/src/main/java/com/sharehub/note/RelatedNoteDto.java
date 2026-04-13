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
    public RelatedNoteDto withTags(List<String> nextTags) {
        return new RelatedNoteDto(
            id,
            title,
            summary,
            updatedAt,
            status,
            category,
            nextTags,
            ownerKey,
            ownerName,
            ownerAvatarUrl,
            favorites,
            favorited
        );
    }
}
