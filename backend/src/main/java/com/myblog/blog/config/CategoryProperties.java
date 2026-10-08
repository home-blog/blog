package com.myblog.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 분류 이름 글자 수 (application.yml의 category.*, specs/003 plan.md 설정값 목록). 글자는 코드 포인트로 센다. */
@ConfigurationProperties(prefix = "category")
public record CategoryProperties(Name name) {

    /** category.name 칸의 길이 (V2__auth_tables.sql의 VARCHAR(20)). */
    public static final int NAME_COLUMN_LENGTH = 20;

    /** 이름 1~20자 (앞뒤 공백을 지운 뒤, FR-036). */
    public record Name(int minLength, int maxLength) {

        public Name {
            if (minLength < 1 || maxLength < minLength || maxLength > NAME_COLUMN_LENGTH) {
                throw new IllegalStateException("category.name은 1 <= min-length <= max-length <= %d이어야 합니다"
                        .formatted(NAME_COLUMN_LENGTH));
            }
        }
    }
}
