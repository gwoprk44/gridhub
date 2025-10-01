package com.gridhub.gridhub.domain.user.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") // DB 테이블 이름을 명시적으로 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String bio; // 자기소개

    private String profileImageUrl;

    @Enumerated(EnumType.STRING) // Enum 타입을 DB에 저장할 때, Enum의 이름을 String으로 저장
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private int points = 0; // 포인트, 기본값 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_driver_id")
    private Driver favoriteDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_team_id")
    private Team favoriteTeam;

    // 포인트 추가 메서드
    public void addPoints(int points) {
        this.points += points;
    }

    @Builder
    public User(String email, String password, String nickname, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    // === 프로필 수정을 위한 업데이트 메서드 ===
    public void updateProfile(String nickname, String bio, Driver favoriteDriver, Team favoriteTeam) {
        this.nickname = nickname;
        this.bio = bio;
        this.favoriteDriver = favoriteDriver;
        this.favoriteTeam = favoriteTeam;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
