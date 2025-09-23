package com.gridhub.gridhub.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content,

        @Nullable // 대댓글이 아닐 경우 null이 될 수 있음
        Long parentCommentId
) {}