package com.myblog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @PasswordConfirmed 검사. 틀리면 confirmField 칸(기본 passwordConfirm)의 오류로 남긴다. */
public class PasswordConfirmedValidator implements ConstraintValidator<PasswordConfirmed, PasswordConfirmation> {

    private final UserInputRules rules;
    private String confirmField;

    public PasswordConfirmedValidator(UserInputRules rules) {
        this.rules = rules;
    }

    @Override
    public void initialize(PasswordConfirmed annotation) {
        this.confirmField = annotation.confirmField();
    }

    @Override
    public boolean isValid(PasswordConfirmation value, ConstraintValidatorContext context) {
        if (value == null || rules.isPasswordConfirmed(value.password(), value.passwordConfirm())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(confirmField)
                .addConstraintViolation();
        return false;
    }
}
