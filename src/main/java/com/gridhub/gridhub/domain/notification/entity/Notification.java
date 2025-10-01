package com.gridhub.gridhub.domain.notification.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // 알림을 받는 사람

    @Column(nullable = false)
    private String content; // "Nickname님이 '게시글 제목'에 댓글을 남겼습니다."

    @Column(nullable = false)
    private String url; // 클릭시 이동할 url

    @Column(nullable = false)
    private boolean isRead = false; // 읽음 여부

    @Builder
    public Notification(User receiver, String content, String url) {
        this.receiver = receiver;
        this.content = content;
        this.url = url;
    }

    public void read() {
        this.isRead = true;
    }
}
