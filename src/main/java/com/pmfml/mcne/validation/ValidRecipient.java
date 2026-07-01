package com.pmfml.mcne.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecipientValidator.class)
@Documented
public @interface ValidRecipient {
    String message() default "Invalid recipient format for the selected channel";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
