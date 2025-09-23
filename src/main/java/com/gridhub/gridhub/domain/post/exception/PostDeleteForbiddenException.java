package com.gridhub.gridhub.domain.post.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class PostDeleteForbiddenException extends BusinessException {
    public PostDeleteForbiddenException() {
        super(ErrorCode.POST_DELETE_FORBIDDEN);
    }
}