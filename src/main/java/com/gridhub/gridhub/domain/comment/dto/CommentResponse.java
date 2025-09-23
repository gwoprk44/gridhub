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
    // 엔티티를 계층형 DTO로 변환하는 정적 팩토리 메서드
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getNickname(),
                comment.getCreatedAt(),
                // 자식 댓글(대댓글)들도 재귀적으로 DTO로 변환
                comment.getChildren().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList())
        );
    }
}