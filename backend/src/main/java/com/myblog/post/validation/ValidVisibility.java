package com.myblog.post.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 공개 여부: "public" / "private". 없으면 통과(공개로 저장, FR-013). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VisibilityValidator.class)
public @interface ValidVisibility {

    String message() default "VISIBILITY_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
