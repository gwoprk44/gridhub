package com.gridhub.gridhub.domain.post.dto;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;

import java.time.LocalDateTime;

public record PostSimpleResponse(
        Long postId,
        String title,
        String authorNickname,
        PostCategory category,
        int viewCount,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static  PostSimpleResponse from(Post post) {
        return new PostSimpleResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getNickname(),
                post.getCategory(),
                post.getViewCount(),
                post.getImageUrl(),
                post.getCreatedAt()
        );
    }
}
