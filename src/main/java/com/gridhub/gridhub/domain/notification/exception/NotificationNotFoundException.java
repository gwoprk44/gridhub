package com.gridhub.gridhub.domain.notification.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class NotificationNotFoundException extends BusinessException {
    public NotificationNotFoundException() {
        super(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}