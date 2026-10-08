package com.myblog.blog.validation;

import com.myblog.blog.config.CategoryProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidCategoryName 검사. 글자는 코드 포인트로 센다. */
public class CategoryNameValidator implements ConstraintValidator<ValidCategoryName, String> {

    private final CategoryProperties properties;
    private boolean optional;

    public CategoryNameValidator(CategoryProperties properties) {
        this.properties = properties;
    }

    @Override
    public void initialize(ValidCategoryName annotation) {
        this.optional = annotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null && optional) {
            return true;
        }
        int length = BlogRuleViolations.strippedLength(value);
        if (length == 0) {
            return BlogRuleViolations.reject(context, "CATEGORY_NAME_REQUIRED");
        }
        if (length < properties.name().minLength() || length > properties.name().maxLength()) {
            return BlogRuleViolations.reject(context, "CATEGORY_NAME_TOO_LONG");
        }
        return true;
    }
}
