package com.gridhub.gridhub.domain.comment.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class CommentDeleteForbiddenException extends BusinessException {
    public CommentDeleteForbiddenException() {
        super(ErrorCode.COMMENT_DELETE_FORBIDDEN);
    }
}
