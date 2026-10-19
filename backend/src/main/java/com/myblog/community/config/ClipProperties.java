package com.myblog.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 문장 수집의 숫자 (application.yml의 community.clip.*, specs/015 US3). 글자는 코드 포인트로 센다.
 * maxLength: 한 문장(300자), maxPerMember: 한 사람이 모을 수 있는 수(1,000), pageSize: 필사 노트 한 쪽(20).
 */
@ConfigurationProperties(prefix = "community.clip")
public record ClipProperties(int maxLength, int maxPerMember, int pageSize) {

    /** sentence_clip.text 칸의 길이 (V17__sentence_clip.sql의 VARCHAR(300)). 설정값은 이보다 클 수 없다. */
    public static final int TEXT_COLUMN_LENGTH = 300;

    public ClipProperties {
        if (maxLength < 1 || maxLength > TEXT_COLUMN_LENGTH) {
            throw new IllegalStateException("community.clip.max-length는 1 이상 %d 이하여야 합니다".formatted(TEXT_COLUMN_LENGTH));
        }
        if (maxPerMember < 1 || pageSize < 1) {
            throw new IllegalStateException("community.clip.max-per-member와 page-size는 1 이상이어야 합니다");
        }
    }
}
