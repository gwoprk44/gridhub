// src/main/java/com/gridhub/gridhub/domain/user/service/UserService.java
package com.gridhub.f1.domain.user.service;

import com.gridhub.f1.domain.user.dto.LoginRequest;
import com.gridhub.f1.domain.user.dto.SignUpRequest;
import com.gridhub.f1.domain.user.entity.User;
import com.gridhub.f1.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.f1.domain.user.exception.InvalidPasswordException;
import com.gridhub.f1.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.f1.domain.user.exception.UserNotFoundException;
import com.gridhub.f1.domain.user.repository.UserRepository;
import com.gridhub.f1.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
    
    /*
    * 로그인 메서드
    * */
    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        // 1. 사용자 확인
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(UserNotFoundException::new);

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 3. JWT 토큰 생성 및 반환
        return jwtUtil.createToken(user.getEmail(), user.getRole());
    }
}