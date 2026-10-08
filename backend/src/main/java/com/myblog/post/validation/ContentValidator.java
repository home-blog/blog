package com.myblog.post.validation;

import com.myblog.post.config.PostProperties;
import com.myblog.post.domain.Post;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @ValidContent 검사. 저장할 모양(줄바꿈만 \n으로 맞춘 원문)으로 센다: 화면에 그려진 글자가 아니라
 * 마크다운 기호와 이미지 주소까지 모두 센다 (003 D-2). 글자는 코드 포인트로 센다.
 */
public class ContentValidator implements ConstraintValidator<ValidContent, String> {

    private final PostProperties properties;

    public ContentValidator(PostProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String content = Post.normalizeContent(value);
        if (content.isBlank()) {
            return PostRuleViolations.reject(context, "CONTENT_REQUIRED");
        }
        int length = content.codePointCount(0, content.length());
        if (length < properties.content().minLength() || length > properties.content().maxLength()) {
            return PostRuleViolations.reject(context, "CONTENT_TOO_LONG");
        }
        return true;
    }
}
