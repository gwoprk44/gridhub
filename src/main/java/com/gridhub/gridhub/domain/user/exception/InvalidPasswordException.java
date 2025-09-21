package com.gridhub.gridhub.domain.user.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() { super(ErrorCode.INVALID_PASSWORD); }
}