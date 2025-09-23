package com.gridhub.gridhub.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용은 비워둘 수 없습니다.")
        @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다.")
        String content
) {
}