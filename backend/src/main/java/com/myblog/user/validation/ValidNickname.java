package com.myblog.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** UserInputRules의 nickname 규칙. message는 ErrorCode 이름이다 (GlobalExceptionHandler가 문구로 바꾼다). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NicknameValidator.class)
public @interface ValidNickname {

    String message() default "INVALID_NICKNAME";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
