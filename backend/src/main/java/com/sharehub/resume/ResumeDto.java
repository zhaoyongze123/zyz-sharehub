package com.sharehub.resume;

import java.time.Instant;
import java.util.UUID;

public record ResumeDto(
    Long id,
    String templateKey,
    String status,
    UUID fileId,
    String fileUrl,
    String fileName,
    Long fileSize,
    Instant fileCreatedAt,
    Instant fileUpdatedAt
) {
}
