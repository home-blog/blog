package com.myblog.comment.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 댓글 규칙의 숫자 (application.yml의 community.comment.*, specs/005 plan.md 설정값 목록, 헌법 VI).
 * 글자는 코드 포인트로 센다 (이모지 하나 = 한 글자, research B-2).
 */
@ConfigurationProperties(prefix = "community.comment")
public record CommentProperties(int minLength, int maxLength, Duration minInterval) {

    /** comment.body 칸의 길이 (V4__comment.sql의 VARCHAR(500)). 설정값은 이보다 클 수 없다. */
    public static final int BODY_COLUMN_LENGTH = 500;

    public CommentProperties {
        if (minLength < 1 || maxLength < minLength || maxLength > BODY_COLUMN_LENGTH) {
            throw new IllegalStateException("community.comment는 1 <= min-length <= max-length <= %d이어야 합니다"
                    .formatted(BODY_COLUMN_LENGTH));
        }
        if (minInterval == null || minInterval.isNegative()) {
            throw new IllegalStateException("community.comment.min-interval은 0 이상이어야 합니다");
        }
    }
}
