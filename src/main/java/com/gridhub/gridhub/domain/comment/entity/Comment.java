package com.gridhub.gridhub.domain.comment.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @Builder
    public Comment(String content, User author, Post post, Comment parent) {
        this.content = content;
        this.author = author;
        this.post = post;
        if (parent != null) {
            this.setParent(parent);
        }
    }

    // === 연관관계 편의 메서드 ===
    public void setParent(Comment parent) {
        this.parent = parent;
        parent.getChildren().add(this);
    }

    public void update(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}