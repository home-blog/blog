package com.myblog.guestbook.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 방명록의 숫자 (application.yml의 guestbook.*, specs/021 research D-4). 글자는 코드 포인트로 센다.
 * 칸 길이(V30__guestbook.sql)보다 긴 한도는 저장할 때 깨지므로 서버가 켜지지 않게 막는다.
 */
@ConfigurationProperties(prefix = "guestbook")
public record GuestbookProperties(int pageSize, int bodyMaxLength, Duration minInterval, int dailyPerBlog,
        ZoneId dayZone) {

    /** guestbook_entry.body·reply VARCHAR(300). */
    public static final int BODY_COLUMN_LENGTH = 300;

    public GuestbookProperties {
        if (pageSize < 1 || dailyPerBlog < 1) {
            throw new IllegalStateException("guestbook.page-size와 daily-per-blog는 1 이상이어야 합니다");
        }
        if (bodyMaxLength < 1 || bodyMaxLength > BODY_COLUMN_LENGTH) {
            throw new IllegalStateException(
                    "guestbook.body-max-length는 1 이상 %d 이하여야 합니다".formatted(BODY_COLUMN_LENGTH));
        }
        if (minInterval == null || minInterval.isNegative() || dayZone == null) {
            throw new IllegalStateException("guestbook.min-interval은 0 이상이고 day-zone이 있어야 합니다");
        }
    }
}
