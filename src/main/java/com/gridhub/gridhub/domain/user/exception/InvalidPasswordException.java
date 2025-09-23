package com.gridhub.f1.domain.user.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() { super(ErrorCode.INVALID_PASSWORD); }
}