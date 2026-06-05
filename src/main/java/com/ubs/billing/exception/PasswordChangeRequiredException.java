package com.ubs.billing.exception;

import org.springframework.http.HttpStatus;

public class PasswordChangeRequiredException extends BaseException {

    public PasswordChangeRequiredException() {
        super(
                "Password change required. Please change your temporary password before accessing other resources.",
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN
        );
    }
}
