package com.myblog.community.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 신고 사유: SPAM, ABUSE, ADULT, OTHER 중 하나. 없거나 다른 값이면 REPORT_REASON_REQUIRED (FR-018). */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReportReasonValidator.class)
public @interface ValidReportReason {

    String message() default "REPORT_REASON_REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
