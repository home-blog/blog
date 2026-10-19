package com.myblog.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.ai.service.ProofreadChecker.Correction;
import com.myblog.ai.service.Proofreader.Suggestion;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 빨간 펜 검사기 (specs/014 S1-2, S1-3, D-3, D-4): AI가 준 고칠 곳을 믿지 않고 본문에서 다시 찾는다.
 * 위치는 보낸 본문의 UTF-16 위치(자바스크립트 문자열 위치와 같음)다.
 */
class ProofreadCheckerTest {

    private static final int REASON_MAX = 120;

    private final ProofreadChecker checker = new ProofreadChecker(100, 200, REASON_MAX);

    private static Suggestion s(String kind, String context, String original, String replacement) {
        return new Suggestion(kind, context, original, replacement, "이유");
    }

    @Test
    void 앞_글자로_본문에서_위치를_찾는다() {
        String text = "오늘은 할수있다고 믿었다.";
        List<Correction> found = checker.check(text, List.of(s("spacing", "오늘은 ", "할수있다고", "할 수 있다고")));

        assertThat(found).singleElement().satisfies(c -> {
            assertThat(c.start()).isEqualTo(4);
            assertThat(c.end()).isEqualTo(9);
            assertThat(text.substring(c.start(), c.end())).isEqualTo("할수있다고");
            assertThat(c.kind()).isEqualTo("spacing");
            assertThat(c.replacement()).isEqualTo("할 수 있다고");
        });
    }

    @Test
    void 같은_원문이_여러_번이면_앞_글자가_맞는_곳을_고른다() {
        String text = "됬다. 그리고 또 됬다.";
        List<Correction> found = checker.check(text, List.of(s("spelling", "그리고 또 ", "됬다", "됐다")));

        assertThat(found).singleElement().satisfies(c -> assertThat(c.start()).isEqualTo(10));
    }

    @Test
    void 앞_글자가_틀려도_원문이_있으면_아직_고르지_않은_첫_자리를_쓴다() {
        String text = "됬다. 그리고 또 됬다.";
        List<Correction> found = checker.check(text, List.of(
                s("spelling", "엉뚱한 앞 ", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다")));

        assertThat(found).extracting(Correction::start).containsExactly(0, 10);
    }

    @Test
    void 본문에_없는_원문_같은_말_빈_원문_모르는_종류는_버린다() {
        String text = "글을 썼다.";
        List<Correction> found = checker.check(text, List.of(
                s("spelling", "", "없는말", "있는 말"),
                s("spelling", "", "썼다", "썼다"),
                s("spelling", "", "", "무엇"),
                s("grammar", "", "글을", "글을 ")));

        assertThat(found).isEmpty();
    }

    @Test
    void 겹치는_고칠_곳은_먼저_온_것만_남긴다() {
        String text = "할수있다";
        List<Correction> found = checker.check(text, List.of(
                s("spacing", "", "할수있다", "할 수 있다"),
                s("spacing", "", "수있", "수 있")));

        assertThat(found).singleElement().satisfies(c -> assertThat(c.original()).isEqualTo("할수있다"));
    }

    @Test
    void 결과는_앞에서부터_정렬한다() {
        String text = "됬다 그리고 할수있다";
        List<Correction> found = checker.check(text, List.of(
                s("spacing", "", "할수있다", "할 수 있다"),
                s("spelling", "", "됬다", "됐다")));

        assertThat(found).extracting(Correction::start).containsExactly(0, 7);
    }

    @Test
    void 코드_블록과_인라인_코드와_링크_주소_안은_고치지_않는다() {
        String text = """
                됬다 `됬다` 끝
                ```
                됬다
                ```
                [됬다](https://example.com/됬다) <https://example.com/됬다> https://example.com/됬다
                """;
        List<Correction> found = checker.check(text, List.of(
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다"),
                s("spelling", "", "됬다", "됐다")));

        // 맨 앞과 링크 글자([됬다])만 고친다. 코드·주소 안의 다섯 곳은 건드리지 않는다
        assertThat(found).extracting(Correction::start)
                .containsExactly(0, text.indexOf("[됬다]") + 1);
    }

    @Test
    void 이모지가_있어도_자바스크립트와_같은_위치를_준다() {
        String text = "😀 할수있다";
        List<Correction> found = checker.check(text, List.of(s("spacing", "😀 ", "할수있다", "할 수 있다")));

        // 😀는 UTF-16 두 칸이다
        assertThat(found).singleElement().satisfies(c -> assertThat(c.start()).isEqualTo(3));
    }

    @Test
    void 고칠_곳은_정한_수까지만_돌려준다() {
        ProofreadChecker small = new ProofreadChecker(2, 200, REASON_MAX);
        String text = "됬다 됬다 됬다";
        List<Correction> found = small.check(text, List.of(
                s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다")));

        assertThat(found).hasSize(2);
    }

    @Test
    void 이유가_너무_길면_자른다() {
        String text = "됬다";
        List<Correction> found = checker.check(text,
                List.of(new Suggestion("spelling", "", "됬다", "됐다", "가".repeat(500))));

        assertThat(found).singleElement().satisfies(c -> assertThat(c.reason()).hasSize(REASON_MAX));
    }

    @Test
    void 코드_블록_안의_언어_표시_줄은_블록을_닫지_않는다() {
        // CommonMark: 닫는 줄은 여는 표시와 같은 글자로 같거나 더 길고, 뒤에는 공백만 있어야 한다
        String text = """
                ````
                ```js
                됬다
                ```
                됬다
                ````
                바깥 됬다
                """;
        List<Correction> found = checker.check(text, List.of(
                s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다")));

        assertThat(found).extracting(Correction::start).containsExactly(text.indexOf("바깥 됬다") + 3);
    }

    @Test
    void 닫는_표시_뒤에_글자가_있으면_닫지_않는다() {
        String text = "```\n됬다\n``` 아님\n됬다\n```\n밖 됬다";
        List<Correction> found = checker.check(text, List.of(
                s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다"), s("spelling", "", "됬다", "됐다")));

        assertThat(found).extracting(Correction::start).containsExactly(text.indexOf("밖 됬다") + 2);
    }

    @Test
    void 원문이_정한_길이를_넘으면_받지_않는다() {
        ProofreadChecker narrow = new ProofreadChecker(100, 4, REASON_MAX);
        String text = "할수있다 그리고 할수있었다";
        List<Correction> found = narrow.check(text, List.of(
                s("spacing", "", "할수있다", "할 수 있다"),
                s("spacing", "", "할수있었다", "할 수 있었다")));

        // 네 글자는 받고, 다섯 글자는 버린다
        assertThat(found).singleElement().satisfies(c -> assertThat(c.original()).isEqualTo("할수있다"));
    }
}
