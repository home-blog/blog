package com.myblog.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 블로그 이름·소개 글자 수를 한곳에서 읽는다 (application.yml의 blog.*, specs/003 plan.md 설정값 목록, 헌법 VI).
 * 글자는 코드 포인트로 센다 (이모지 하나 = 한 글자). DB 칸보다 길게 정하면 검사는 통과하고 저장에서 실패하므로,
 * 서버가 켜질 때 거절한다.
 */
@ConfigurationProperties(prefix = "blog")
public record BlogProperties(Name name, Intro intro) {

    /** blog.name 칸의 길이 (V2__auth_tables.sql의 VARCHAR(30)). */
    public static final int NAME_COLUMN_LENGTH = 30;

    /** blog.intro 칸의 길이 (V2__auth_tables.sql의 VARCHAR(500)). 서버 검사는 이보다 짧은 200자다 (FR-005). */
    public static final int INTRO_COLUMN_LENGTH = 500;

    /** 이름 1~30자 (앞뒤 공백을 지운 뒤, FR-004). */
    public record Name(int minLength, int maxLength) {

        public Name {
            if (minLength < 1 || maxLength < minLength || maxLength > NAME_COLUMN_LENGTH) {
                throw new IllegalStateException("blog.name은 1 <= min-length <= max-length <= %d이어야 합니다"
                        .formatted(NAME_COLUMN_LENGTH));
            }
        }
    }

    /** 소개 0~200자 (FR-005). */
    public record Intro(int minLength, int maxLength) {

        public Intro {
            if (minLength < 0 || maxLength < minLength || maxLength > INTRO_COLUMN_LENGTH) {
                throw new IllegalStateException("blog.intro는 0 <= min-length <= max-length <= %d이어야 합니다"
                        .formatted(INTRO_COLUMN_LENGTH));
            }
        }
    }
}
