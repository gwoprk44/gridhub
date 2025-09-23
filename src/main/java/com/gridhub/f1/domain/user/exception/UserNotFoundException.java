package com.gridhub.f1.domain.user.exception;

import com.gridhub.f1.global.exception.BusinessException;
import com.gridhub.f1.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() { super(ErrorCode.USER_NOT_FOUND); }
}