package com.gridhub.gridhub.domain.post.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class PostUpdateForbiddenException extends BusinessException {
    public PostUpdateForbiddenException() {
        super(ErrorCode.POST_UPDATE_FORBIDDEN);
    }
}
