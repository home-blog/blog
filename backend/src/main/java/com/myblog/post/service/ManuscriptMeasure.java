package com.myblog.post.service;

import com.myblog.post.config.ManuscriptProperties;
import org.springframework.stereotype.Component;

/**
 * 글의 원고지 매수와 읽는 시간 (specs/007 US3). 둘 다 올림이고 최소 1이다.
 * 글자 수는 목록 미리보기와 같은 규칙으로 마크다운 기호를 걷어 낸 본문의 코드 포인트 수다 (PostPreview.plainText).
 */
@Component
public class ManuscriptMeasure {

    private final int charsPerSheet;
    private final int charsPerMinute;

    public ManuscriptMeasure(ManuscriptProperties properties) {
        this.charsPerSheet = properties.charsPerSheet();
        this.charsPerMinute = properties.charsPerMinute();
    }

    public Measure of(String markdown) {
        String text = PostPreview.plainText(markdown);
        int chars = text.codePointCount(0, text.length());
        return new Measure(chars, ceilAtLeastOne(chars, charsPerSheet), ceilAtLeastOne(chars, charsPerMinute));
    }

    private static int ceilAtLeastOne(int chars, int per) {
        return Math.max(1, (chars + per - 1) / per);
    }

    /** chars는 센 글자 수, sheets는 원고지 매수, readMinutes는 읽는 시간(분). */
    public record Measure(int chars, int sheets, int readMinutes) {
    }
}
