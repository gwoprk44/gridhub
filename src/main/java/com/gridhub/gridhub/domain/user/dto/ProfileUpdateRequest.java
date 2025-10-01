package com.gridhub.gridhub.domain.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
    private String nickname;
    @Size(max = 100, message = "자기소개는 100자를 넘을 수 없습니다.")
    private String bio;
    private Integer favoriteDriverId; // 드라이버 ID (driver_number)
    private Long favoriteTeamId;      // 팀 ID
}