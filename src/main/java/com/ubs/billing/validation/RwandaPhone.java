package com.ubs.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RwandaPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RwandaPhone {

    String message() default "Phone number must be a valid Rwanda mobile number (e.g. +2507XXXXXXXX or 07XXXXXXXX)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
