package com.myblog.post.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 글 본문 (post.content.*). 저장하는 마크다운 원문 그대로 센다 (기호·이미지 주소 포함, 003 D-2).
 * 공백·줄바꿈만 있으면 비어 있는 것으로 본다. 비면 CONTENT_REQUIRED, 길면 CONTENT_TOO_LONG (FR-011).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContentValidator.class)
public @interface ValidContent {

    String message() default "CONTENT_REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
