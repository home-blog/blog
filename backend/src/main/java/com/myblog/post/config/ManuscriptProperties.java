package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 원고지 매수·읽는 시간 (application.yml의 post.manuscript.*, specs/007 US3, 가안).
 * 글자 수는 마크다운 기호를 걷어 낸 본문을 코드 포인트로 센다 (띄어쓰기 포함).
 *
 * @param charsPerSheet  원고지 한 장의 칸 수 (200자 원고지)
 * @param charsPerMinute 1분에 읽는 글자 수
 */
@ConfigurationProperties(prefix = "post.manuscript")
public record ManuscriptProperties(int charsPerSheet, int charsPerMinute) {

    public ManuscriptProperties {
        if (charsPerSheet < 1 || charsPerMinute < 1) {
            throw new IllegalStateException("post.manuscript.chars-per-sheet, chars-per-minute는 1 이상이어야 합니다");
        }
    }
}
