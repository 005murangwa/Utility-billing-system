package com.ubs.billing.validation;

import com.ubs.billing.dto.request.CreateMeterReadingRequest;
import com.ubs.billing.dto.request.UpdateMeterReadingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class ValidMeterReadingValuesValidator implements ConstraintValidator<ValidMeterReadingValues, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal previousReading;
        BigDecimal currentReading;

        if (value instanceof CreateMeterReadingRequest request) {
            previousReading = request.getPreviousReading();
            currentReading = request.getCurrentReading();
        } else if (value instanceof UpdateMeterReadingRequest request) {
            previousReading = request.getPreviousReading();
            currentReading = request.getCurrentReading();
        } else {
            return true;
        }

        if (previousReading == null || currentReading == null) {
            return true;
        }

        return currentReading.compareTo(previousReading) > 0;
    }
}
