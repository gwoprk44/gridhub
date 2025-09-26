package com.gridhub.gridhub.domain.f1data.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class DriverNotFoundException extends BusinessException {
    public DriverNotFoundException() { super(ErrorCode.DRIVER_NOT_FOUND); }
}