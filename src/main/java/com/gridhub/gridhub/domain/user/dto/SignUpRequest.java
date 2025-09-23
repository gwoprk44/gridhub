package com.gridhub.gridhub.domain.user.dto;

import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;

public record SignUpRequest(
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$", message = "유효하지 않은 이메일 형식입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,16}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~16자리여야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        // 2~10자의 영문 대소문자, 숫자, 한글만 허용
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,10}$", message = "닉네임은 2~10자의 영문, 숫자, 한글만 사용 가능합니다.")
        String nickname
) {
    //DTO를 엔티티로 변환하는 메서드
    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password)) // 비밀번호 암호화
                .nickname(this.nickname)
                .role(UserRole.USER) // 기본 역할은 유저
                .build();
    }
}
