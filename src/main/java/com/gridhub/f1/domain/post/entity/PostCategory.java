package com.gridhub.f1.domain.post.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostCategory {
    FREE("자유게시판"),
    INFO("정보/기술"),
    RUMOR("루머/이슈");

    private final String title;
}
