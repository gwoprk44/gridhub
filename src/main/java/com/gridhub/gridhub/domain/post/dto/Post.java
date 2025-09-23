package com.gridhub.gridhub.domain.post.dto;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Column(nullable = false)
    private int viewCount = 0;

    // Post가 연관관계의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author; // 작성자

    @Builder
    public Post(String title, String content, PostCategory category, User author) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.author = author;
    }
}
