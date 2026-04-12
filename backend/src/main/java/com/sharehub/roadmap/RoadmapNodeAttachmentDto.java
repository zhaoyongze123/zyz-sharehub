package com.sharehub.roadmap;

import java.time.Instant;
import java.util.UUID;

public record RoadmapNodeAttachmentDto(
    UUID id,
    String filename,
    String contentType,
    long size,
    String downloadUrl,
    Instant createdAt
) {}
