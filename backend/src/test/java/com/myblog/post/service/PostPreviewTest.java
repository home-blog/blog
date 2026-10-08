package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 본문 앞부분 (specs/004 T008, research B-3, D-8의 B, FR-005, quickstart S-1의 8·9). */
class PostPreviewTest {

    private final PostPreview preview = new PostPreview(100);

    @Test
    void 줄바꿈이_섞인_150자는_공백으로_이어_100자와_말줄임표다() {
        String body = "가".repeat(50) + "\r\n" + "나".repeat(50) + "\n" + "다".repeat(30) + "\r" + "라".repeat(17);
        String result = preview.of(body);
        assertThat(result).doesNotContain("\n").doesNotContain("\r").endsWith(PostPreview.ELLIPSIS);
        assertThat(result.codePointCount(0, result.length())).isEqualTo(101);
        assertThat(result).startsWith("가".repeat(50) + " " + "나".repeat(49));
    }

    @Test
    void 짧은_본문은_그대로이고_말줄임표가_없다() {
        String body = "짧은 글 ".repeat(16);
        assertThat(preview.of(body)).isEqualTo(body.strip());
        assertThat(preview.of("가".repeat(100))).isEqualTo("가".repeat(100));
    }

    @Test
    void 이모지는_한_글자로_세고_반으로_자르지_않는다() {
        String result = preview.of("😀".repeat(120));
        assertThat(result).isEqualTo("😀".repeat(100) + PostPreview.ELLIPSIS);
    }

    @Test
    void 마크다운_기호를_걷어_낸다() {
        String body = """
                # 가을 산책
                > 인용한 말
                - **굵게** 쓴 *기울임* 과 `코드`
                1. [링크 글자](https://example.com) 와 ![사진 설명](https://example.com/a.png)
                ---
                ```java
                int a = 1;
                ```
                snake_case_name 은 그대로
                """;
        assertThat(preview.of(body))
                .isEqualTo("가을 산책 인용한 말 굵게 쓴 기울임 과 코드 링크 글자 와 사진 설명 int a = 1; snake_case_name 은 그대로");
    }

    @Test
    void 기호만_있는_줄은_빈_글자가_된다() {
        assertThat(preview.of("---\n\n***")).isEmpty();
        assertThat(preview.of("#")).isEqualTo("#");
    }

    @Test
    void HTML은_글자_그대로_남긴다() {
        assertThat(preview.of("<script>alert(1)</script>")).isEqualTo("<script>alert(1)</script>");
    }
}
