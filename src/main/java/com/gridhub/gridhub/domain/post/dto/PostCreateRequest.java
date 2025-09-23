package com.gridhub.gridhub.domain.post.dto;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 512, message = "제목은 512자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @NotNull(message = "카테고리를 선택해주세요.")
        PostCategory category
) {
    // DTO를 엔티티로 변환하는 메서드. 작성자(author) 정보를 인자로 받는다.
    public Post toEntity(User author) {
        return Post.builder()
                .title(this.title)
                .content(this.content)
                .category(this.category)
                .author(author)
                .build();
    }
}