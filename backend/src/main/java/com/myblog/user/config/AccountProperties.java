package com.myblog.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 계정 관리 규칙의 숫자를 한곳에서 읽는다 (application.yml의 account.*, specs/002 plan.md 설정값 목록).
 * 닉네임·비밀번호 규칙은 가입과 같아서 AuthProperties를 그대로 쓴다.
 */
@ConfigurationProperties(prefix = "account")
public record AccountProperties(Intro intro) {

    /** users.intro 칸의 길이 (V2__auth_tables.sql의 VARCHAR(100)). 설정값은 이보다 클 수 없다. */
    public static final int INTRO_COLUMN_LENGTH = 100;

    /**
     * 소개 글자 수 (0~100자). 글자는 코드 포인트로 센다 (이모지 하나 = 한 글자).
     * DB 칸보다 길게 정하면 검사는 통과하고 저장에서 실패하므로, 서버가 켜질 때 거절한다.
     */
    public record Intro(int minLength, int maxLength) {

        public Intro {
            if (minLength < 0 || maxLength < minLength || maxLength > INTRO_COLUMN_LENGTH) {
                throw new IllegalStateException("account.intro는 0 <= min-length <= max-length <= %d이어야 합니다"
                        .formatted(INTRO_COLUMN_LENGTH));
            }
        }
    }
}
