package com.myblog.post.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 글 목록·검색 설정을 잘못 정하면 서버가 켜지지 않는다 (specs/004 T004). */
class ExplorePropertiesTest {

    @Test
    void 기본값은_된다() {
        assertThatCode(() -> new ExploreProperties(new ExploreProperties.ListPage(10, 100),
                new ExploreProperties.Search(2, 50))).doesNotThrowAnyException();
    }

    @Test
    void 영이나_거꾸로_정한_값은_거절한다() {
        assertThatThrownBy(() -> new ExploreProperties.ListPage(0, 100)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ExploreProperties.ListPage(10, 0)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ExploreProperties.Search(0, 50)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ExploreProperties.Search(5, 2)).isInstanceOf(IllegalStateException.class);
    }
}
