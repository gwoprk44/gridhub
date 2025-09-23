package com.gridhub.f1.domain.post.dto;

import com.gridhub.f1.domain.post.entity.Post;
import com.gridhub.f1.domain.post.entity.PostCategory;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        String title,
        String content,
        String authorNickname,
        PostCategory category,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getNickname(), // 지연 로딩 발생 지점
                post.getCategory(),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}