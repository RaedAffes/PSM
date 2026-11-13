package com.psm.dto;

import com.psm.entity.TaskComment;
import java.time.Instant;

public record TaskCommentResponse(
    Long id,
    Long taskId,
    Long authorId,
    String authorName,
    String content,
    Instant createdAt,
    Instant updatedAt
) {

    public static TaskCommentResponse from(TaskComment comment) {
        var author = comment.getAuthor();
        return new TaskCommentResponse(
            comment.getId(),
            comment.getTask().getId(),
            author == null ? null : author.getId(),
            author == null ? null : (author.getNom() + " " + author.getPrenom()).trim(),
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
