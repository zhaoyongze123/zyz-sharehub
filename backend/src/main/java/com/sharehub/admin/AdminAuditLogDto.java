package com.sharehub.admin;

import java.time.Instant;

public record AdminAuditLogDto(
    Long id,
    String action,
    String targetType,
    String targetId,
    String operatorKey,
    String details,
    Instant createdAt
) {
}
