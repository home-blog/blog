package com.myblog.blog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 분류 이름 (category.name.*). 앞뒤 공백을 지운 뒤 센다. 비면 CATEGORY_NAME_REQUIRED, 길면 CATEGORY_NAME_TOO_LONG (FR-036). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CategoryNameValidator.class)
public @interface ValidCategoryName {

    String message() default "CATEGORY_NAME_REQUIRED";

    /** 참이면 없음(null)을 "바꾸지 않음"으로 보고 통과시킨다 (분류 고치기에서 보낸 칸만 바꿀 때). */
    boolean optional() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
