package com.ubs.billing.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends BaseException {

    public AccountNotVerifiedException(String message) {
        super(message, HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_NOT_VERIFIED);
    }
}
