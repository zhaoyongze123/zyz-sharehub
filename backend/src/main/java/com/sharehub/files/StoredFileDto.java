package com.sharehub.files;

import java.time.Instant;
import java.util.UUID;

public record StoredFileDto(
    UUID id,
    String owner,
    FileCategory category,
    String referenceType,
    String referenceId,
    String filename,
    String contentType,
    long size,
    String checksum,
    String downloadUrl,
    Instant createdAt
) {
}
