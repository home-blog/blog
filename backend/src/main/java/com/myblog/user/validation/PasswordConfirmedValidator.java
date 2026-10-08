package com.myblog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @PasswordConfirmed 검사. 틀리면 passwordConfirm 칸의 오류로 남긴다. */
public class PasswordConfirmedValidator implements ConstraintValidator<PasswordConfirmed, PasswordConfirmation> {

    private final UserInputRules rules;

    public PasswordConfirmedValidator(UserInputRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean isValid(PasswordConfirmation value, ConstraintValidatorContext context) {
        if (value == null || rules.isPasswordConfirmed(value.password(), value.passwordConfirm())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("passwordConfirm")
                .addConstraintViolation();
        return false;
    }
}
