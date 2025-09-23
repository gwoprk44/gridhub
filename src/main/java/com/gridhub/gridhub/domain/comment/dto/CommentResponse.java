package com.gridhub.gridhub.domain.comment.dto;

import com.gridhub.gridhub.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record CommentResponse(
        Long commentId,
        String content,
        String authorNickname,
        LocalDateTime createdAt,
        List<CommentResponse> replies // 대댓글 목록
) {
    private static final String DELETED_MESSAGE = "[삭제된 댓글입니다.]";

    // 엔티티를 계층형 DTO로 변환하는 정적 팩토리 메서드
    public static CommentResponse from(Comment comment) {
        String content = comment.isDeleted() ? DELETED_MESSAGE : comment.getContent();
        String authorNickname = comment.isDeleted() ? "" : comment.getAuthor().getNickname();

        return new CommentResponse(
                comment.getId(),
                content,
                authorNickname,
                comment.getCreatedAt(),
                comment.getChildren().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList())
        );
    }
}