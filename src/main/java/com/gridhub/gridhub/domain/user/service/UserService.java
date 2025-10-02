package com.gridhub.gridhub.domain.user.service;

import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import com.gridhub.gridhub.domain.user.dto.ProfileResponse;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.exception.NicknameAlreadyExistsException;
import com.gridhub.gridhub.domain.user.exception.UserNotFoundException;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploaderService s3UploaderService;
    private final DriverRepository driverRepository;
    private final TeamRepository teamRepository;

    /**
     * 내 프로필 조회
     */
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);
        return ProfileResponse.from(user);
    }

    /**
     * 내 프로필 수정
     */
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
            if (user.getProfileImageUrl() != null) {
                s3UploaderService.delete(user.getProfileImageUrl());
            }
            String newImageUrl = s3UploaderService.upload(profileImage);
            user.updateProfileImage(newImageUrl);
        }

        // 3. 선호 드라이버/팀 ID 유효성 검증 및 엔티티 조회
        Driver favoriteDriver = (request.getFavoriteDriverId() != null)
                ? driverRepository.findById(request.getFavoriteDriverId()).orElse(null)
                : user.getFavoriteDriver();
        Team favoriteTeam = (request.getFavoriteTeamId() != null)
                ? teamRepository.findById(request.getFavoriteTeamId()).orElse(null)
                : user.getFavoriteTeam();

        // 4. 나머지 텍스트 정보 업데이트
        user.updateProfile(
                request.getNickname() != null ? request.getNickname() : user.getNickname(),
                request.getBio() != null ? request.getBio() : user.getBio(),
                favoriteDriver,
                favoriteTeam
        );
    }
}