package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 검색어 다듬기 (specs/004 T027, research B-4, B-5, R-1). */
class SearchKeywordTest {

    @Test
    void 앞뒤_공백과_전각_공백을_지우고_가운데_공백까지_센다() {
        SearchKeyword keyword = SearchKeyword.of(" 　 단풍  명소\t ");
        assertThat(keyword.keyword()).isEqualTo("단풍  명소");
        assertThat(keyword.length()).isEqualTo(6);
        assertThat(keyword.words()).containsExactly("단풍", "명소");
    }

    @Test
    void 탭과_전각_공백으로도_나누고_같은_단어는_하나로() {
        assertThat(SearchKeyword.of("spring\tboot　spring").words()).containsExactly("spring", "boot");
    }

    @Test
    void 없거나_공백만이면_빈_검색어() {
        assertThat(SearchKeyword.of(null).length()).isZero();
        assertThat(SearchKeyword.of("   ").words()).isEmpty();
    }

    @Test
    void 이모지는_한_글자다() {
        assertThat(SearchKeyword.of("😀😀").length()).isEqualTo(2);
    }

    @Test
    void 퍼센트_밑줄_역슬래시는_일반_글자로_바꾼다() {
        assertThat(SearchKeyword.of("100% a_b c\\d").containsPatterns())
                .containsExactly("%100\\%%", "%a\\_b%", "%c\\\\d%");
    }
}
