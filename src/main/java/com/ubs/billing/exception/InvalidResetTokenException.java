package com.ubs.billing.exception;

import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends BaseException {

    public InvalidResetTokenException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.RESET_TOKEN_INVALID);
    }
}
