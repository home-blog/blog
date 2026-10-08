package com.myblog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidPassword 검사. 규칙은 UserInputRules에 있다. */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private final UserInputRules rules;

    public PasswordValidator(UserInputRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return rules.isValidPassword(value);
    }
}
