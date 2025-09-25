package com.gridhub.gridhub.domain.f1data.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class RaceNotFoundException extends BusinessException {
    public RaceNotFoundException() {
        super(ErrorCode.RACE_NOT_FOUND);
    }
}
