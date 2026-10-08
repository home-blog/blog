package com.myblog.post.validation;

import com.myblog.common.Visibility;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidVisibility 검사. */
public class VisibilityValidator implements ConstraintValidator<ValidVisibility, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Visibility.from(value).isPresent();
    }
}
