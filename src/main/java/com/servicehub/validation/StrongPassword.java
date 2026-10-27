package com.servicehub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Senha deve ter ao menos 8 caracteres, no máximo 72 bytes UTF-8, uma letra maiúscula e um número";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
