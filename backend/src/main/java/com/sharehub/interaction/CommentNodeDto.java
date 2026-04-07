package com.sharehub.interaction;

import java.util.ArrayList;
import java.util.List;

public record CommentNodeDto(
    Long id,
    Long resourceId,
    Long noteId,
    Long parentId,
    String content,
    String status,
    List<CommentNodeDto> children
) {
    public static CommentNodeDto from(InteractionRepository.CommentRecord record) {
        return new CommentNodeDto(
            record.id(),
            record.resourceId(),
            record.noteId(),
            record.parentId(),
            record.content(),
            record.status(),
            new ArrayList<>()
        );
    }
}
