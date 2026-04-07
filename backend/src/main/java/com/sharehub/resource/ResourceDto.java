package com.sharehub.resource;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record ResourceDto(
    Long id,
    @NotBlank String title,
    String type,
    String category,
    String summary,
    List<String> tags,
    String externalUrl,
    String objectKey,
    String visibility,
    String status,
    Instant updatedAt,
    String author,
    long likes,
    long favorites,
    long downloadCount
) {
    public ResourceDto(
        Long id,
        String title,
        String type,
        String summary,
        List<String> tags,
        String externalUrl,
        String objectKey,
        String visibility,
        String status
    ) {
        this(id, title, type, type, summary, tags, externalUrl, objectKey, visibility, status, null, null, 0L, 0L, 0L);
    }
}
