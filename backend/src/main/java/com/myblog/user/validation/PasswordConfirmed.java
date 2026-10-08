package com.myblog.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 확인이 비밀번호와 같아야 한다 (001 FR-007, 002 FR-017).
 * 오류는 confirmField 칸에 붙는다: 가입은 passwordConfirm, 비밀번호 변경은 newPasswordConfirm.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConfirmedValidator.class)
public @interface PasswordConfirmed {

    String message() default "PASSWORD_MISMATCH";

    /** 오류를 붙일 칸 이름 (요청 본문의 필드 이름). */
    String confirmField() default "passwordConfirm";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
