package com.myblog.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 분류 규칙의 숫자 (application.yml의 category.*, specs/003 plan.md 설정값 목록). 글자는 코드 포인트로 센다.
 * colorCount: 분류 색 목록의 색 개수 (specs/006 T025, D-9). 새 분류는 정해진 순서로 0 ~ colorCount-1의 색 번호를 받는다.
 * 색 값 자체는 화면이 index.css 토큰(--category-1 …)으로 정한다.
 */
@ConfigurationProperties(prefix = "category")
public record CategoryProperties(Name name, int colorCount) {

    public CategoryProperties {
        if (colorCount < 1 || colorCount > Short.MAX_VALUE) {
            throw new IllegalStateException("category.color-count는 1 이상이어야 합니다");
        }
    }

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
