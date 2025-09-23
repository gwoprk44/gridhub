package com.gridhub.f1.domain.comment.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class InvalidParentComment extends BusinessException {
    public InvalidParentComment() {
        super(ErrorCode.INVALID_PARENT_COMMENT);
    }
}
