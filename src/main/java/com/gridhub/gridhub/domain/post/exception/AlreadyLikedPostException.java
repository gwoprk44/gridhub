package com.gridhub.gridhub.domain.post.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class AlreadyLikedPostException extends BusinessException {
    public AlreadyLikedPostException() {
        super(ErrorCode.ALREADY_LIKED_POST);
    }
}