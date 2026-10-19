package com.myblog.editorial.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 편집실의 숫자 (application.yml의 editorial.*, specs/018). pageSize: 검토 대기·처리 기록 한 쪽,
 * reasonMaxLength: 내림·지우기 사유(editorial_action.reason·post.lock_reason 칸 200자 안), dayZone: "오늘"을 세는 날짜 기준.
 */
@ConfigurationProperties(prefix = "editorial")
public record EditorialProperties(int pageSize, int reasonMaxLength, ZoneId dayZone) {

    /** post.lock_reason·editorial_action.reason VARCHAR(200). */
    public static final int REASON_COLUMN_LENGTH = 200;

    public EditorialProperties {
        if (pageSize < 1) {
            throw new IllegalStateException("editorial.page-size는 1 이상이어야 합니다");
        }
        if (reasonMaxLength < 1 || reasonMaxLength > REASON_COLUMN_LENGTH) {
            throw new IllegalStateException(
                    "editorial.reason-max-length는 1 이상 %d 이하여야 합니다".formatted(REASON_COLUMN_LENGTH));
        }
        if (dayZone == null) {
            throw new IllegalStateException("editorial.day-zone이 필요합니다");
        }
    }
}
