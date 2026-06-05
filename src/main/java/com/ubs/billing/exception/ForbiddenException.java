package com.ubs.billing.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }
}
