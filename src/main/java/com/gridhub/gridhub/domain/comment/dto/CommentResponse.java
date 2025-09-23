package com.gridhub.f1.domain.comment.dto;

import com.gridhub.f1.domain.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record CommentResponse(
        Long commentId,
        String content,
        String writerNickname,
        LocalDateTime createdAt,
        boolean isDeleted,
        List<CommentResponse> children
) {
    /**
     * Entity -> DTO 변환을 위한 정적 팩토리 메서드
     */
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                // 삭제된 댓글은 작성자 닉네임을 "알 수 없음" 또는 null로 처리
                comment.getWriter() != null ? comment.getWriter().getNickname() : "알 수 없음",
                comment.getCreatedAt(),
                comment.isDeleted(),
                // 자식 댓글들도 재귀적으로 DTO로 변환
                comment.getChildren().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList())
        );
    }
}