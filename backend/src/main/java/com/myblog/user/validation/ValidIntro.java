package com.myblog.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 소개 글자 수 (account.intro.*). 비어 있거나 없어도 된다. message는 ErrorCode 이름이다. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IntroValidator.class)
public @interface ValidIntro {

    String message() default "INTRO_TOO_LONG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
