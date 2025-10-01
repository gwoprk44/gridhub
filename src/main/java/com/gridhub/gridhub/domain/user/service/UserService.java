// src/main/java/com/gridhub/gridhub/domain/user/service/UserService.java
package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import com.gridhub.gridhub.domain.user.dto.LoginRequest;
import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.dto.SignUpRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.EmailAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.InvalidPasswordException;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final S3UploaderService s3UploaderService;
    private final DriverRepository driverRepository;
    private final TeamRepository teamRepository;

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

    /*
    * 프로필 조회
    * */
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        return ProfileResponse.from(user);
    }

    /*
    * 프로필 수정
    * */
    @Transactional
    public void updateMyProfile(String userEmail, ProfileUpdateRequest request, MultipartFile profileImage) throws IOException {
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);

        // 1. 닉네임 변경 시 중복 확인
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new NicknameAlreadyExistsException();
            }
        }

        // 2. 프로필 이미지 변경 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 있으면 S3에서 삭제
            if (user.getProfileImageUrl() != null) {
                s3UploaderService.delete(user.getProfileImageUrl());
            }
            // 새 이미지 업로드 후 URL 설정
            String newImageUrl = s3UploaderService.upload(profileImage);

            user.updateProfileImage(newImageUrl);
        }

        // 3. 선호 드라이버/팀 ID 유효성 검증 및 엔티티 조회
        Driver favoriteDriver = (request.getFavoriteDriverId() != null)
                ? driverRepository.findById(request.getFavoriteDriverId()).orElse(null)
                : user.getFavoriteDriver(); // 요청에 없으면 기존 값 유지
        Team favoriteTeam = (request.getFavoriteTeamId() != null)
                ? teamRepository.findById(request.getFavoriteTeamId()).orElse(null)
                : user.getFavoriteTeam(); // 요청에 없으면 기존 값 유지

        // 4. 나머지 텍스트 정보 업데이트
        user.updateProfile(
                request.getNickname() != null ? request.getNickname() : user.getNickname(),
                request.getBio() != null ? request.getBio() : user.getBio(),
                favoriteDriver,
                favoriteTeam
        );
    }
}