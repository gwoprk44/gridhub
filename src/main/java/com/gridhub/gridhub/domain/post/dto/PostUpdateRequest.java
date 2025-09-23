package com.gridhub.gridhub.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 512, message = "제목은 512자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content
) {
}
