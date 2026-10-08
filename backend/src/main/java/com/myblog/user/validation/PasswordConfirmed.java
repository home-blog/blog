package com.myblog.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 비밀번호 확인이 비밀번호와 같아야 한다 (FR-007). 오류는 passwordConfirm 칸에 붙는다. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConfirmedValidator.class)
public @interface PasswordConfirmed {

    String message() default "PASSWORD_MISMATCH";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
