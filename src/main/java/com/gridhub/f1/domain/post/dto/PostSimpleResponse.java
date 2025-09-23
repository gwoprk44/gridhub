package com.gridhub.f1.domain.post.dto;

import com.gridhub.f1.domain.post.entity.Post;
import com.gridhub.f1.domain.post.entity.PostCategory;

import java.time.LocalDateTime;

public record PostSimpleResponse(
        Long postId,
        String title,
        String authorNickname,
        PostCategory category,
        int viewCount,
        LocalDateTime createdAt
) {
    public static  PostSimpleResponse from(Post post) {
        return new PostSimpleResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getNickname(),
                post.getCategory(),
                post.getViewCount(),
                post.getCreatedAt()
        );
    }
}
