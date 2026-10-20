package com.myblog.user.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 개인 열쇠 규칙의 숫자 (application.yml의 access-key.*, specs/020 data-model). */
@ConfigurationProperties(prefix = "access-key")
public record AccessKeyProperties(int maxPerMember, List<Integer> allowedDays, Duration touchInterval,
        int nameMaxLength) {

    /** access_key.name 칸 길이 (V27). 설정을 이보다 길게 정하면 서버가 켜지지 않는다. */
    static final int NAME_COLUMN_LENGTH = 40;

    public AccessKeyProperties {
        if (maxPerMember < 1 || allowedDays == null || allowedDays.isEmpty()
                || allowedDays.stream().anyMatch(days -> days == null || days < 1)) {
            throw new IllegalStateException("access-key: max-per-member >= 1, allowed-days는 1 이상의 날 수여야 합니다");
        }
        allowedDays = List.copyOf(allowedDays);
        if (touchInterval == null || touchInterval.isNegative()) {
            throw new IllegalStateException("access-key.touch-interval은 0 이상이어야 합니다");
        }
        if (nameMaxLength < 1 || nameMaxLength > NAME_COLUMN_LENGTH) {
            throw new IllegalStateException(
                    "access-key.name-max-length는 1 이상 %d 이하여야 합니다".formatted(NAME_COLUMN_LENGTH));
        }
    }
}
