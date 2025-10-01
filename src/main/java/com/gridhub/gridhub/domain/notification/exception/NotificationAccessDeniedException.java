package com.gridhub.gridhub.domain.notification.exception;

import com.gridhub.gridhub.global.exception.BusinessException;
import com.gridhub.gridhub.global.exception.ErrorCode;

public class NotificationAccessDeniedException extends BusinessException {
    public NotificationAccessDeniedException() {
        super(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }
}