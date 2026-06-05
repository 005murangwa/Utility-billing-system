package com.ubs.billing.validation;

import com.ubs.billing.util.Constants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class StaffRoleValidator implements ConstraintValidator<ValidStaffRole, Object> {

    private static final Set<String> STAFF_ROLES = Set.of(
            Constants.ROLE_OPERATOR,
            Constants.ROLE_FINANCE,
            Constants.ROLE_ADMIN
    );

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value instanceof String role) {
            return STAFF_ROLES.contains(role);
        }

        if (value instanceof Set<?> roles) {
            return !roles.isEmpty() && roles.stream().allMatch(role -> role instanceof String && STAFF_ROLES.contains(role));
        }

        return false;
    }
}
