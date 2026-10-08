package com.myblog.blog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 블로그 소개 (blog.intro.*). 비어도 된다. 앞뒤 공백을 지운 뒤 센다 (research B-4 가안 해석). 길면 BLOG_INTRO_TOO_LONG (FR-005).
 * DB 칸은 500자지만 서버가 200자로 막는다.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BlogIntroValidator.class)
public @interface ValidBlogIntro {

    String message() default "BLOG_INTRO_TOO_LONG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
