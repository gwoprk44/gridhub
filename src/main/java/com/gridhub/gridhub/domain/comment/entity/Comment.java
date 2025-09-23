package com.gridhub.gridhub.domain.comment.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean isDeleted = false; // 삭제 여부 플래그 (기본값 : false)

    /*
    * 연관관계
    * */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 삭제된 댓글의 경우 writer가 null이 될 수 있으므로 nullable = true (혹은 더미 유저)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /*
    * 대댓글을 위한 자기참조
    * */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // 부모 댓글

    @OneToMany(mappedBy = "parent")
    private List<Comment> children = new ArrayList<>(); // 자식 댓글들

    /*
    * 생성자
    * */
    @Builder
    public Comment(String content, User writer, Post post, Comment parent) {
        this.content = content;
        this.writer = writer;
        this.post = post;
        this.parent = parent;
        this.isDeleted = false;
    }

    //===비즈니스 로직===//

    /*
    * 댓글 내용 수정
    * */
    public void updateContent(String newContent) {
        this.content = newContent;
    }

    /*
    * 댓글 삭제 처리
    * */
    public void markAsDeleted() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
        this.writer = null; // 작성자 정보 제거
    }
}
