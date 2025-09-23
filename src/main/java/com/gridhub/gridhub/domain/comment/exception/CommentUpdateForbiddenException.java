package com.gridhub.gridhub.domain.comment.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class CommentUpdateForbiddenException extends BusinessException {
    public CommentUpdateForbiddenException() {
        super(ErrorCode.COMMENT_DELETE_FORBIDDEN);
    }
}
