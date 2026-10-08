package com.myblog.blog.validation;

import com.myblog.blog.config.BlogProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidBlogIntro 검사. 없거나 비어도 통과한다. */
public class BlogIntroValidator implements ConstraintValidator<ValidBlogIntro, String> {

    private final BlogProperties properties;

    public BlogIntroValidator(BlogProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        int length = BlogRuleViolations.strippedLength(value);
        return length >= properties.intro().minLength() && length <= properties.intro().maxLength();
    }
}
