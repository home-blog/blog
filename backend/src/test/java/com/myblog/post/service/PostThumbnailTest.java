package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostThumbnailTest {

    @Test
    void 이미지가_없으면_비어_있다() {
        assertThat(PostThumbnail.of(null)).isEmpty();
        assertThat(PostThumbnail.of("그림 없는 글. [링크](https://example.com)")).isEmpty();
    }

    @Test
    void 대표가_없으면_첫_이미지() {
        String markdown = "첫 문단\n\n![바다](/api/images/a.jpg)\n\n![산](https://example.com/b.jpg)";
        assertThat(PostThumbnail.of(markdown)).contains("/api/images/a.jpg");
    }

    @Test
    void 대표라고_적은_이미지가_먼저() {
        String markdown = "![바다](/api/images/a.jpg)\n\n![산](https://example.com/b.jpg \"대표\")";
        assertThat(PostThumbnail.of(markdown)).contains("https://example.com/b.jpg");
    }

    @Test
    void 꺾쇠로_감싼_주소와_다른_제목도_읽는다() {
        assertThat(PostThumbnail.of("![a](<https://example.com/c.png> \"노을\")")).contains("https://example.com/c.png");
    }

    @Test
    void 위험하거나_다른_사이트를_가리키는_상대_주소는_건너뛴다() {
        String markdown = "![x](javascript:alert(1)) ![y](//evil.example/a.png) ![z](data:image/png;base64,AAA) "
                + "![ok](/api/images/d.webp)";
        assertThat(PostThumbnail.of(markdown)).contains("/api/images/d.webp");
    }

    @Test
    void 코드_블록과_인라인_코드_안의_이미지_문법은_건너뛴다() {
        String markdown = """
                마크다운 이미지 쓰는 법:

                ```markdown
                ![예시](/api/images/a.png "대표")
                ```

                한 줄로는 `![b](/api/images/b.png)` 처럼 씁니다.

                ![진짜](/api/images/c.png)
                """;
        assertThat(PostThumbnail.of(markdown)).contains("/api/images/c.png");
    }

    @Test
    void 닫히지_않은_코드_블록은_글_끝까지_코드다() {
        assertThat(PostThumbnail.of("~~~\n![a](/api/images/a.png)\n")).isEmpty();
    }
}
