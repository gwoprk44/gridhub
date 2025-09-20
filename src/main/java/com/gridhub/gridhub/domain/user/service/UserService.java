// src/main/java/com/gridhub/gridhub/domain/user/service/UserService.java
package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
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
        // 1. 중복 검사
        validateDuplicateCredentials(request.email(), request.nickname());

        // 2. DTO를 Entity로 변환 (비밀번호 암호화 포함)
        User newUser = request.toEntity(passwordEncoder);

        // 3. DB에 저장
        userRepository.save(newUser);
    }

    /**
     * 이메일과 닉네임의 중복을 검사하는 private 메서드
     * @param email 검사할 이메일
     * @param nickname 검사할 닉네임
     */
    private void validateDuplicateCredentials(String email, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new NicknameAlreadyExistsException();
        }
    }
}