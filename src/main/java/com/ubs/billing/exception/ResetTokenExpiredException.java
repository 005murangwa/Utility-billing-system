package com.ubs.billing.exception;

import org.springframework.http.HttpStatus;

public class ResetTokenExpiredException extends BaseException {

    public ResetTokenExpiredException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.RESET_TOKEN_EXPIRED);
    }
}
