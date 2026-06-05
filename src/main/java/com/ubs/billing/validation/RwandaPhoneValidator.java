package com.ubs.billing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class RwandaPhoneValidator implements ConstraintValidator<RwandaPhone, String> {

    private static final Pattern RWANDA_PHONE_PATTERN =
            Pattern.compile("^(?:\\+250|250|0)?(7[2389]\\d{7})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.replaceAll("\\s+", "");
        return RWANDA_PHONE_PATTERN.matcher(normalized).matches();
    }
}
