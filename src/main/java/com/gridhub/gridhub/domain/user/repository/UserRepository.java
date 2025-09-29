package com.gridhub.gridhub.domain.user.repository;

import com.gridhub.gridhub.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자를 찾는 메서드(로그인시 사용)
    // 결과가 null일수 있음 -> 옵셔널로 감싸서 반환
    Optional<User> findByEmail(String email);

    // 이메일 중복 체크를 위한 메서드
    boolean existsByEmail(String email);

    // 닉네임 중복 체크를 위한 메서드
    boolean existsByNickname(String nickname);

    // points를 기준으로 내림차순 정렬하여 페이징 조회
    Page<User> findAllByOrderByPointsDesc(Pageable pageable);
}
