package com.servicehub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        return value.codePointCount(0, value.length()) >= 8 && bytes <= 72 && value.codePoints().anyMatch(Character::isUpperCase)
                && value.codePoints().anyMatch(Character::isDigit);
    }
}
