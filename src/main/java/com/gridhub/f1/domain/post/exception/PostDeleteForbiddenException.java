package com.gridhub.f1.domain.post.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class PostDeleteForbiddenException extends BusinessException {
    public PostDeleteForbiddenException() {
        super(ErrorCode.POST_DELETE_FORBIDDEN);
    }
}