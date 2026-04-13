package com.sharehub.me;

public record UpdateMeProfileRequest(
    String displayName,
    String bio
) {
}
