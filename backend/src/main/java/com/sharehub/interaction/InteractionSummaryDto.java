package com.sharehub.interaction;

public record InteractionSummaryDto(
    Long resourceId,
    long comments,
    long favorites,
    long likes,
    long reports
) {
}
