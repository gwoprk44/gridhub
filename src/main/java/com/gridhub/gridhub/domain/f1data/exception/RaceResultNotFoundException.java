package com.gridhub.gridhub.domain.f1data.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class RaceResultNotFoundException extends BusinessException {
    public RaceResultNotFoundException() {
        super(ErrorCode.RACE_RESULT_NOT_FOUND);
    }
}
