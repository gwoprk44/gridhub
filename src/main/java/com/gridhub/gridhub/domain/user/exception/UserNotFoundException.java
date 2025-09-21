package com.gridhub.gridhub.domain.user.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() { super(ErrorCode.USER_NOT_FOUND); }
}