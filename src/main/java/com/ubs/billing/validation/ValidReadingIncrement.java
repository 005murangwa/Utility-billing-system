package com.ubs.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ReadingIncrementValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReadingIncrement {

    String message() default "Current reading must be greater than previous reading";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
