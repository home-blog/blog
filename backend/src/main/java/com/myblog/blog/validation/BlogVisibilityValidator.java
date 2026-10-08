package com.myblog.blog.validation;

import com.myblog.common.Visibility;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidBlogVisibility 검사. */
public class BlogVisibilityValidator implements ConstraintValidator<ValidBlogVisibility, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Visibility.from(value).isPresent();
    }
}
