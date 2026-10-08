package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** 페이지 계산 (specs/004 T007, T037, FR-002, FR-004, SC-005, D-2: A). */
class PageNumbersTest {

    @Test
    void 글이_0개면_1페이지뿐이다() {
        assertThat(PageNumbers.of("1", 0, 10)).isEqualTo(new PageNumbers.PageSlice(1, 1, 10));
    }

    @Test
    void 글_25개는_3페이지이고_3페이지는_20개를_건너뛴다() {
        PageNumbers.PageSlice slice = PageNumbers.of("3", 25, 10);
        assertThat(slice.totalPages()).isEqualTo(3);
        assertThat(slice.page()).isEqualTo(3);
        assertThat(slice.offset()).isEqualTo(20);
        assertThat(PageNumbers.of("1", 30, 10).totalPages()).isEqualTo(3);
        assertThat(PageNumbers.of("1", 31, 10).totalPages()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"99", "4", "999999999", "99999999999999999999999"})
    void 마지막보다_큰_번호는_마지막_페이지다(String page) {
        assertThat(PageNumbers.of(page, 25, 10).page()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-1", "abc", "1 OR 1=1", "2.5", "00", " 2", "１"})
    void 없거나_1보다_작거나_숫자가_아니면_1페이지다(String page) {
        assertThat(PageNumbers.of(page, 25, 10).page()).isEqualTo(1);
    }

    @Test
    void 앞의_0은_무시한다() {
        assertThat(PageNumbers.of("02", 25, 10).page()).isEqualTo(2);
    }
}
