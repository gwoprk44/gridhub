package com.gridhub.gridhub.domain.user.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
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

    // 선호 드라이버/팀은 ID만 우선 저장 (추후 Driver, Team 엔티티와 연관관계 매핑 가능)
    private Long favoriteDriverId;
    private Long favoriteTeamId;

    @Builder
    public User(String email, String password, String nickname, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }
}
