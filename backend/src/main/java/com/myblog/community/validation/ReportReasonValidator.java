package com.myblog.community.validation;

import com.myblog.community.domain.ReportReason;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidReportReason 검사 (specs/005 T036). */
public class ReportReasonValidator implements ConstraintValidator<ValidReportReason, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return ReportReason.parse(value).isPresent();
    }
}
