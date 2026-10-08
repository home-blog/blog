package com.myblog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidNickname 검사. 규칙은 UserInputRules에 있다. */
public class NicknameValidator implements ConstraintValidator<ValidNickname, String> {

    private final UserInputRules rules;

    public NicknameValidator(UserInputRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return rules.isValidNickname(value);
    }
}
