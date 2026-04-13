package com.sharehub.interaction;

public record NoteInteractionSummaryDto(
    Long noteId,
    long favorites,
    long likes,
    long reports
) {
}
