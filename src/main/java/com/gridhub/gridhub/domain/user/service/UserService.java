package com.gridhub.gridhub.domain.user.service;


import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            // TODO: 추후 커스텀 예외로 변경
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 닉네임 중복 확인
        if (userRepository.existsByNickname(request.nickname())) {
            // TODO: 추후 커스텀 예외로 변경
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3. DTO를 Entity로 변환 (비밀번호 암호화 포함)
        User newUser = request.toEntity(passwordEncoder);

        // 4. DB에 저장
        userRepository.save(newUser);
    }
}