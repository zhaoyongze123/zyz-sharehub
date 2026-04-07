package com.sharehub.me;

import com.sharehub.auth.UserProfileDto;

public record MeDto(
    UserProfileDto profile,
    long myResourceCount,
    long myFavoriteCount,
    long myRoadmapCount,
    long myNoteCount,
    long myResumeCount
) {
}
