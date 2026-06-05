package com.ubs.billing.validation;

import com.ubs.billing.dto.request.MeterReadingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class MeterReadingValuesValidator implements ConstraintValidator<ValidMeterReadingValues, MeterReadingRequest> {

    @Override
    public boolean isValid(MeterReadingRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getPreviousReading() == null || request.getCurrentReading() == null) {
            return true;
        }

        BigDecimal previous = request.getPreviousReading();
        BigDecimal current = request.getCurrentReading();

        if (current.compareTo(previous) <= 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Current reading must be greater than previous reading")
                    .addPropertyNode("currentReading")
                    .addConstraintViolation();
            return false;
        }

        if (request.getReadingDate() != null && request.getMonth() != null && request.getYear() != null) {
            if (request.getReadingDate().getMonthValue() != request.getMonth()
                    || request.getReadingDate().getYear() != request.getYear()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Reading date must match the specified month and year")
                        .addPropertyNode("readingDate")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
