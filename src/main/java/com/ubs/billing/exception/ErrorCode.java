package com.ubs.billing.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR("VALIDATION_ERROR"),
    BAD_REQUEST("BAD_REQUEST"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    NOT_FOUND("NOT_FOUND"),
    CONFLICT("CONFLICT"),
    ACCOUNT_NOT_VERIFIED("ACCOUNT_NOT_VERIFIED"),
    OTP_EXPIRED("OTP_EXPIRED"),
    INVALID_OTP("INVALID_OTP"),
    TOKEN_INVALID("TOKEN_INVALID"),
    RESET_TOKEN_INVALID("RESET_TOKEN_INVALID"),
    RESET_TOKEN_EXPIRED("RESET_TOKEN_EXPIRED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;
}
