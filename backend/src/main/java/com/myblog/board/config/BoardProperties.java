package com.myblog.board.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 글마당의 숫자 (application.yml의 board.*, specs/017). 글자는 코드 포인트로 센다.
 * 칸 길이(V21__board.sql)보다 긴 한도는 저장할 때 깨지므로 서버가 켜지지 않게 막는다.
 */
@ConfigurationProperties(prefix = "board")
public record BoardProperties(int pageSize, int recentSize, int titleMaxLength, int contentMaxLength,
        int commentMaxLength, Duration postInterval, Duration commentInterval, int keywordMaxLength) {

    /** board_post.title VARCHAR(100). */
    public static final int TITLE_COLUMN_LENGTH = 100;
    /** board_comment.content VARCHAR(1000). */
    public static final int COMMENT_COLUMN_LENGTH = 1000;

    public BoardProperties {
        if (pageSize < 1 || recentSize < 1 || contentMaxLength < 1 || keywordMaxLength < 1) {
            throw new IllegalStateException(
                    "board.page-size, recent-size, content-max-length, keyword-max-length는 1 이상이어야 합니다");
        }
        if (titleMaxLength < 1 || titleMaxLength > TITLE_COLUMN_LENGTH) {
            throw new IllegalStateException("board.title-max-length는 1 이상 %d 이하여야 합니다".formatted(TITLE_COLUMN_LENGTH));
        }
        if (commentMaxLength < 1 || commentMaxLength > COMMENT_COLUMN_LENGTH) {
            throw new IllegalStateException(
                    "board.comment-max-length는 1 이상 %d 이하여야 합니다".formatted(COMMENT_COLUMN_LENGTH));
        }
        if (postInterval == null || postInterval.isNegative() || commentInterval == null || commentInterval.isNegative()) {
            throw new IllegalStateException("board.post-interval과 comment-interval은 0 이상이어야 합니다");
        }
    }
}
