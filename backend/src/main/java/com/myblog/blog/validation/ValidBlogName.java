package com.myblog.blog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 블로그 이름 (blog.name.*). 앞뒤 공백을 지운 뒤 센다. 비면 BLOG_NAME_REQUIRED, 길면 BLOG_NAME_TOO_LONG (FR-004). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BlogNameValidator.class)
public @interface ValidBlogName {

    String message() default "BLOG_NAME_REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
