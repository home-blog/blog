package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 태그 규칙의 숫자 (application.yml의 community.tag.*, specs/005 plan.md 설정값 목록, 헌법 VI).
 * 글자는 코드 포인트로 센다. 태그별 글 목록의 페이지 글 수는 004 목록(explore.list.page-size)과 같은 값을 빌려 쓴다.
 */
@ConfigurationProperties(prefix = "community.tag")
public record TagProperties(int maxPerPost, int minLength, int maxLength, int pageSize) {

    /** tag.name 칸의 길이 (V7__tag.sql의 VARCHAR(15)). 설정값은 이보다 클 수 없다. */
    public static final int NAME_COLUMN_LENGTH = 15;

    public TagProperties {
        if (maxPerPost < 0) {
            throw new IllegalStateException("community.tag.max-per-post는 0 이상이어야 합니다");
        }
        if (minLength < 1 || maxLength < minLength || maxLength > NAME_COLUMN_LENGTH) {
            throw new IllegalStateException("community.tag는 1 <= min-length <= max-length <= %d이어야 합니다"
                    .formatted(NAME_COLUMN_LENGTH));
        }
        if (pageSize < 1) {
            throw new IllegalStateException("community.tag.page-size는 1 이상이어야 합니다");
        }
    }
}
