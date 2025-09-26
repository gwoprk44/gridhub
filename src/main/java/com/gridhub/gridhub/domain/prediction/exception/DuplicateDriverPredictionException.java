package com.gridhub.gridhub.domain.prediction.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class DuplicateDriverPredictionException extends BusinessException {
    public DuplicateDriverPredictionException() { super(ErrorCode.DUPLICATE_DRIVER_PREDICTION); }
}