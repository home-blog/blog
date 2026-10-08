package com.myblog.blog.validation;

import com.myblog.blog.config.BlogProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidBlogName 검사. 글자는 코드 포인트로 센다. */
public class BlogNameValidator implements ConstraintValidator<ValidBlogName, String> {

    private final BlogProperties properties;

    public BlogNameValidator(BlogProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        int length = BlogRuleViolations.strippedLength(value);
        if (length == 0) {
            return BlogRuleViolations.reject(context, "BLOG_NAME_REQUIRED");
        }
        if (length < properties.name().minLength() || length > properties.name().maxLength()) {
            return BlogRuleViolations.reject(context, "BLOG_NAME_TOO_LONG");
        }
        return true;
    }
}
