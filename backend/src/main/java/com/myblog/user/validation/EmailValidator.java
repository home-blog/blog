package com.myblog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidEmail 검사. 규칙은 UserInputRules에 있다. */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private final UserInputRules rules;

    public EmailValidator(UserInputRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return rules.isValidEmail(value);
    }
}
