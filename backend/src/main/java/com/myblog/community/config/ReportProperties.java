package com.myblog.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 신고 규칙의 숫자 (application.yml의 community.report.*, specs/005 plan.md 설정값 목록, 헌법 VI).
 * 글자는 코드 포인트로 센다.
 */
@ConfigurationProperties(prefix = "community.report")
public record ReportProperties(int detailMaxLength) {

    /** post_report.detail 칸의 길이 (V6__post_report.sql의 VARCHAR(200)). 설정값은 이보다 클 수 없다. */
    public static final int DETAIL_COLUMN_LENGTH = 200;

    public ReportProperties {
        if (detailMaxLength < 0 || detailMaxLength > DETAIL_COLUMN_LENGTH) {
            throw new IllegalStateException("community.report.detail-max-length는 0 이상 %d 이하여야 합니다"
                    .formatted(DETAIL_COLUMN_LENGTH));
        }
    }
}
