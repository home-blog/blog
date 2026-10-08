package com.myblog.blog.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myblog.post.config.PostProperties;
import org.junit.jupiter.api.Test;

/** 블로그·분류·글 글자 수 설정은 DB 칸을 넘을 수 없다. 잘못 정하면 서버가 켜지지 않는다 (specs/003 T005). */
class BlogPropertiesTest {

    @Test
    void 기본값은_된다() {
        assertThatCode(() -> new BlogProperties.Name(1, 30)).doesNotThrowAnyException();
        assertThatCode(() -> new BlogProperties.Intro(0, 200)).doesNotThrowAnyException();
        assertThatCode(() -> new CategoryProperties.Name(1, 20)).doesNotThrowAnyException();
        assertThatCode(() -> new PostProperties.Title(1, 100)).doesNotThrowAnyException();
        assertThatCode(() -> new PostProperties.Content(1, 10000)).doesNotThrowAnyException();
    }

    @Test
    void DB_칸보다_길거나_거꾸로_정하면_켜지지_않는다() {
        assertThatThrownBy(() -> new BlogProperties.Name(1, 31)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new BlogProperties.Name(0, 30)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new BlogProperties.Intro(0, 501)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CategoryProperties.Name(1, 21)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PostProperties.Title(1, 101)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PostProperties.Content(10, 5)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 분류_색_개수는_1개_이상이어야_한다() {
        CategoryProperties.Name name = new CategoryProperties.Name(1, 20);
        assertThatCode(() -> new CategoryProperties(name, 6)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new CategoryProperties(name, 0)).isInstanceOf(IllegalStateException.class);
    }
}
