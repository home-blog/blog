package com.myblog.post.validation;

import jakarta.validation.ConstraintValidatorContext;

/** 검사 하나가 상황에 따라 다른 오류 이름(ErrorCode)을 내게 한다 (예: 비었음 / 너무 김). */
final class PostRuleViolations {

    private PostRuleViolations() {
    }

    static boolean reject(ConstraintValidatorContext context, String errorCode) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorCode).addConstraintViolation();
        return false;
    }
}
