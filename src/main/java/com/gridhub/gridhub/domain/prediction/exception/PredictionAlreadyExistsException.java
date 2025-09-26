package com.gridhub.gridhub.domain.prediction.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class PredictionAlreadyExistsException extends BusinessException {
    public PredictionAlreadyExistsException() { super(ErrorCode.PREDICTION_ALREADY_EXISTS); }
}