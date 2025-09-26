package com.gridhub.gridhub.domain.prediction.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class PredictionPeriodInvalidException extends BusinessException {
    public PredictionPeriodInvalidException() { super(ErrorCode.PREDICTION_PERIOD_INVALID); }
}