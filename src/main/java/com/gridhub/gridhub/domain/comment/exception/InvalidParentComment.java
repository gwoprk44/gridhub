package com.gridhub.gridhub.domain.comment.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class InvalidParentComment extends BusinessException {
    public InvalidParentComment() {
        super(ErrorCode.INVALID_PARENT_COMMENT);
    }
}
