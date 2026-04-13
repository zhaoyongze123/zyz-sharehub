package com.sharehub.auth;

import java.util.UUID;

public record UserProfileDto(
    Long id,
    String login,
    String name,
    String bio,
    UUID avatarFileId,
    String avatarUrl,
    String status,
    boolean isAdmin,
    boolean isSuperAdmin
) {
}
