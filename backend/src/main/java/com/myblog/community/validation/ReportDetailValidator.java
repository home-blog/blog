package com.myblog.community.validation;

import com.myblog.community.config.ReportProperties;
import com.myblog.community.domain.PostReport;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidReportDetail 검사 (specs/005 T036). 저장할 모양(PostReport.normalizeDetail)으로 센다. */
public class ReportDetailValidator implements ConstraintValidator<ValidReportDetail, String> {

    private final ReportProperties properties;

    public ReportDetailValidator(ReportProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String detail = PostReport.normalizeDetail(value);
        return detail.codePointCount(0, detail.length()) <= properties.detailMaxLength();
    }
}
