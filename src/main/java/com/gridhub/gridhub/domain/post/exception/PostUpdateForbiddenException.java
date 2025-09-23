package com.gridhub.f1.domain.post.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class PostUpdateForbiddenException extends BusinessException {
    public PostUpdateForbiddenException() {
        super(ErrorCode.POST_UPDATE_FORBIDDEN);
    }
}
