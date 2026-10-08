package com.myblog.post.validation;

import com.myblog.post.config.PostProperties;
import com.myblog.post.domain.Post;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidTitle 검사. 글자는 코드 포인트로 센다 (이모지 하나 = 한 글자, research B-4). */
public class TitleValidator implements ConstraintValidator<ValidTitle, String> {

    private final PostProperties properties;

    public TitleValidator(PostProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String title = Post.normalizeTitle(value);
        int length = title.codePointCount(0, title.length());
        if (length == 0) {
            return PostRuleViolations.reject(context, "TITLE_REQUIRED");
        }
        if (length < properties.title().minLength() || length > properties.title().maxLength()) {
            return PostRuleViolations.reject(context, "TITLE_TOO_LONG");
        }
        return true;
    }
}
