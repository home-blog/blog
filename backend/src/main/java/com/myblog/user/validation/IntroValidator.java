package com.myblog.user.validation;

import com.myblog.user.config.AccountProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @ValidIntro 검사. 글자는 코드 포인트로 센다: Java의 length()는 이모지 하나를 2로 셀 수 있다 (specs/002 research B-4).
 * DB의 varchar(100)도 글자 단위로 센다.
 */
public class IntroValidator implements ConstraintValidator<ValidIntro, String> {

    private final AccountProperties properties;

    public IntroValidator(AccountProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.codePointCount(0, value.length());
        return length >= properties.intro().minLength() && length <= properties.intro().maxLength();
    }
}
