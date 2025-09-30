package com.gridhub.gridhub.domain.post.dto;

import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequestDto {
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 512)
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotNull(message = "카테고리를 선택해주세요.")
    private PostCategory category;

    public Post toEntity(User author, String imageUrl) {
        return Post.builder()
                .title(this.title)
                .content(this.content)
                .category(this.category)
                .author(author)
                .imageUrl(imageUrl)
                .build();
    }
}