package com.gridhub.f1.domain.user.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class NicknameAlreadyExistsException extends BusinessException {

    public NicknameAlreadyExistsException() {
        super(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }
}