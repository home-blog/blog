package com.myblog.blog.validation;

import jakarta.validation.ConstraintValidatorContext;

/** 검사 하나가 상황에 따라 다른 오류 이름(ErrorCode)을 내게 한다 (예: 비었음 / 너무 김). */
final class BlogRuleViolations {

    private BlogRuleViolations() {
    }

    static boolean reject(ConstraintValidatorContext context, String errorCode) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorCode).addConstraintViolation();
        return false;
    }

    /** 앞뒤 공백을 지운 글자 수 (코드 포인트). 없으면 0. */
    static int strippedLength(String value) {
        if (value == null) {
            return 0;
        }
        String stripped = value.strip();
        return stripped.codePointCount(0, stripped.length());
    }
}
