package com.myblog.blog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 분류의 공개 여부: "public" / "private". 없으면 통과 (추가 때는 공개, 고칠 때는 그대로, FR-047). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BlogVisibilityValidator.class)
public @interface ValidBlogVisibility {

    String message() default "VISIBILITY_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
