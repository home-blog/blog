package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.post.config.ManuscriptProperties;
import org.junit.jupiter.api.Test;

/** 원고지 매수·읽는 시간 (specs/007 US3): 마크다운 기호를 뺀 글자 수 ÷ 200(매), ÷ 500(분), 올림, 최소 1. */
class ManuscriptMeasureTest {

    private final ManuscriptMeasure measure = new ManuscriptMeasure(new ManuscriptProperties(200, 500));

    @Test
    void 짧은_글도_한_매_일_분() {
        ManuscriptMeasure.Measure m = measure.of("안녕");
        assertThat(m.chars()).isEqualTo(2);
        assertThat(m.sheets()).isEqualTo(1);
        assertThat(m.readMinutes()).isEqualTo(1);
    }

    @Test
    void 이백자는_한_매_이백한자는_두_매() {
        assertThat(measure.of("가".repeat(200)).sheets()).isEqualTo(1);
        assertThat(measure.of("가".repeat(201)).sheets()).isEqualTo(2);
        assertThat(measure.of("가".repeat(1001)).readMinutes()).isEqualTo(3);
    }

    @Test
    void 마크다운_기호는_세지_않고_띄어쓰기는_센다() {
        // "# 제목\n**굵게** 글" → "제목 굵게 글" (띄어쓰기 2개 포함 7자)
        assertThat(measure.of("# 제목\n**굵게** 글").chars()).isEqualTo(7);
    }

    @Test
    void 이모지도_한_글자() {
        assertThat(measure.of("😀😀").chars()).isEqualTo(2);
    }

    @Test
    void 빈_본문도_최소값() {
        ManuscriptMeasure.Measure m = measure.of(null);
        assertThat(m.chars()).isZero();
        assertThat(m.sheets()).isEqualTo(1);
        assertThat(m.readMinutes()).isEqualTo(1);
    }
}
