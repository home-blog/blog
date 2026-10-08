package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 글 제목·본문 글자 수 (application.yml의 post.*, specs/003 plan.md 설정값 목록, 헌법 VI).
 * 글자는 코드 포인트로 센다. 본문은 저장하는 마크다운 원문 그대로 센다 (003 D-2).
 */
@ConfigurationProperties(prefix = "post")
public record PostProperties(Title title, Content content) {

    /** post.title 칸의 길이 (V3__blog_posts.sql의 VARCHAR(100)). */
    public static final int TITLE_COLUMN_LENGTH = 100;

    /** 제목 1~100자 (앞뒤 공백을 지운 뒤, FR-010). */
    public record Title(int minLength, int maxLength) {

        public Title {
            if (minLength < 1 || maxLength < minLength || maxLength > TITLE_COLUMN_LENGTH) {
                throw new IllegalStateException("post.title은 1 <= min-length <= max-length <= %d이어야 합니다"
                        .formatted(TITLE_COLUMN_LENGTH));
            }
        }
    }

    /** 본문 1~10,000자 (FR-011). 본문 칸은 TEXT라 칸 길이 제한은 없다. */
    public record Content(int minLength, int maxLength) {

        public Content {
            if (minLength < 1 || maxLength < minLength) {
                throw new IllegalStateException("post.content는 1 <= min-length <= max-length이어야 합니다");
            }
        }
    }
}
