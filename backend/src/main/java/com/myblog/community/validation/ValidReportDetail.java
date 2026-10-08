package com.myblog.community.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 신고 설명: 0 ~ community.report.detail-max-length자 (앞뒤 공백을 지운 뒤, 코드 포인트로 셈). 넘으면 REPORT_DETAIL_TOO_LONG.
 * 비우거나 보내지 않아도 된다 (FR-018).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReportDetailValidator.class)
public @interface ValidReportDetail {

    String message() default "REPORT_DETAIL_TOO_LONG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
