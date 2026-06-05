package com.ubs.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = StaffRoleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStaffRole {

    String message() default "Role must be ROLE_OPERATOR, ROLE_FINANCE, or ROLE_ADMIN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
