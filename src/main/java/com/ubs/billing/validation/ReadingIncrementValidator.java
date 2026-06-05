package com.ubs.billing.validation;

import com.ubs.billing.dto.request.CreateMeterReadingRequest;
import com.ubs.billing.dto.request.UpdateMeterReadingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class ReadingIncrementValidator implements ConstraintValidator<ValidReadingIncrement, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal previousReading = extractPreviousReading(value);
        BigDecimal currentReading = extractCurrentReading(value);

        if (previousReading == null || currentReading == null) {
            return true;
        }

        return currentReading.compareTo(previousReading) > 0;
    }

    private BigDecimal extractPreviousReading(Object value) {
        if (value instanceof CreateMeterReadingRequest request) {
            return request.getPreviousReading();
        }
        if (value instanceof UpdateMeterReadingRequest request) {
            return request.getPreviousReading();
        }
        return null;
    }

    private BigDecimal extractCurrentReading(Object value) {
        if (value instanceof CreateMeterReadingRequest request) {
            return request.getCurrentReading();
        }
        if (value instanceof UpdateMeterReadingRequest request) {
            return request.getCurrentReading();
        }
        return null;
    }
}
