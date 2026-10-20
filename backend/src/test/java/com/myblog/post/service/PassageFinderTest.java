package com.myblog.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.post.PostLibrary.Passage;
import com.myblog.post.PostLibrary.PassageOptions;
import com.myblog.post.repository.PostLibraryRepository.Found;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 단락 찾기 계산 (specs/020 research D-3). 단락 나누기, 점수, 글마다·전체 상한, 자르기를 본다.
 */
class PassageFinderTest {

    private static final PassageOptions OPTIONS = new PassageOptions(50, 600, 2, 8);

    private static Found post(long id, String title, String content) {
        return new Found(id, title, content, "public", Instant.parse("2026-10-20T00:00:00Z"), "블로그 " + id);
    }

    @Test
    void 빈_줄로_단락을_나누고_코드_블록은_한_덩어리_이미지만_있는_줄은_뺀다() {
        String markdown = "첫 단락\n이어지는 줄\n\n\n![사진](/img/a.png)\n\n```java\nint a = 1;\n\nint b = 2;\n```\n"
                + "끝 단락\r\n";
        List<PassageFinder.Block> blocks = PassageFinder.blocks(markdown);
        assertThat(blocks).extracting(PassageFinder.Block::index).containsExactly(1, 2, 3);
        assertThat(blocks.get(0).text()).isEqualTo("첫 단락\n이어지는 줄");
        assertThat(blocks.get(1).text()).startsWith("```java").contains("int a = 1;\n\nint b = 2;").endsWith("```");
        assertThat(blocks.get(2).text()).isEqualTo("끝 단락");
        assertThat(PassageFinder.blocks("")).isEmpty();
        assertThat(PassageFinder.blocks("![만](/x.png)\n\n   \n")).isEmpty();
    }

    @Test
    void 제목_줄만_있는_덩어리는_다음_단락에_붙이고_끝에_남은_제목은_혼자_단락이_된다() {
        String markdown = "# 들어가며\n\n## 첫날\n\n오늘은 비가 왔다.\n\n#해시태그 같은 줄\n\n### 맺으며";
        List<PassageFinder.Block> blocks = PassageFinder.blocks(markdown);
        assertThat(blocks).extracting(PassageFinder.Block::text)
                .containsExactly("# 들어가며\n## 첫날\n오늘은 비가 왔다.", "#해시태그 같은 줄", "### 맺으며");
        // 제목과 본문이 한 덩어리면 그대로
        assertThat(PassageFinder.blocks("## 제목\n본문")).extracting(PassageFinder.Block::text)
                .containsExactly("## 제목\n본문");
        // 제목에서만 맞아도 본문과 같이 나온다 (AI가 인용할 내용이 있게)
        List<Passage> found = PassageFinder.find(List.of(post(1, "기록", "## 리액트 라우터\n\n로더를 써 봤다.")),
                List.of("리액트"), OPTIONS);
        assertThat(found).singleElement().extracting(Passage::text).isEqualTo("## 리액트 라우터\n로더를 써 봤다.");
    }

    @Test
    void 드문_낱말이_많이_나온_단락이_먼저이고_대소문자를_가리지_않는다() {
        Found lake = post(1, "산책", "호수 둘레를 걸었다.\n\n물안개가 피는 호수. 물안개 사이로 해가 떴다.\n\n점심은 국수.");
        Found city = post(2, "도시", "도시의 아침에도 물안개가 낀다.\n\n지하철은 붐볐다.");
        List<Passage> found = PassageFinder.find(List.of(lake, city), List.of("물안개", "호수"), OPTIONS);
        assertThat(found).hasSize(3);
        assertThat(found.get(0).postId()).isEqualTo(1L);
        assertThat(found.get(0).paragraph()).isEqualTo(2);
        assertThat(found.get(0).url()).isEqualTo("/posts/1");
        assertThat(found).extracting(Passage::score).isSortedAccordingTo((a, b) -> Double.compare(b, a));
        // 맞은 낱말이 없는 단락("점심은 국수", "지하철")은 나오지 않는다
        assertThat(found).extracting(Passage::text).noneMatch(text -> text.contains("국수") || text.contains("지하철"));

        List<Passage> english = PassageFinder.find(List.of(post(3, "Notes", "Spring BOOT tips")), List.of("boot"),
                OPTIONS);
        assertThat(english).singleElement().extracting(Passage::text).isEqualTo("Spring BOOT tips");
    }

    @Test
    void 제목에_낱말이_있는_글의_단락이_같은_점수면_앞선다() {
        Found plain = post(1, "일기", "봄비가 내렸다.");
        Found titled = post(2, "봄비 기록", "봄비가 내렸다.");
        List<Passage> found = PassageFinder.find(List.of(plain, titled), List.of("봄비"), OPTIONS);
        assertThat(found).extracting(Passage::postId).containsExactly(2L, 1L);
        assertThat(found.get(0).score()).isGreaterThan(found.get(1).score());

        // 점수가 같으면 후보 순서(최근 글 먼저)
        List<Passage> tie = PassageFinder.find(List.of(post(5, "가", "봄비"), post(4, "나", "봄비")), List.of("봄비"),
                OPTIONS);
        assertThat(tie).extracting(Passage::postId).containsExactly(5L, 4L);
    }

    @Test
    void 글마다_perPost개_전체_limit개까지만_고른다() {
        String many = "단풍 하나\n\n단풍 둘\n\n단풍 셋\n\n단풍 넷";
        PassageOptions options = new PassageOptions(50, 600, 2, 3);
        List<Passage> found = PassageFinder.find(List.of(post(1, "가을", many), post(2, "가을2", many)),
                List.of("단풍"), options);
        assertThat(found).hasSize(3);
        assertThat(found.stream().filter(p -> p.postId() == 1L)).hasSize(2);
        assertThat(found.stream().filter(p -> p.postId() == 2L)).hasSize(1);
        // 같은 점수면 글 안의 앞 단락 먼저
        assertThat(found.get(0).paragraph()).isEqualTo(1);
        assertThat(found.get(1).paragraph()).isEqualTo(2);
    }

    @Test
    void 한_단락에_같은_낱말이_세_번_넘게_나와도_세_번까지만_센다() {
        Found spam = post(1, "가", "눈 눈 눈 눈 눈 눈 눈 눈");
        Found three = post(2, "나", "눈 눈 눈");
        List<Passage> found = PassageFinder.find(List.of(spam, three), List.of("눈"), OPTIONS);
        assertThat(found.get(0).score()).isEqualTo(found.get(1).score());
    }

    @Test
    void 길면_처음_맞은_곳_둘레로_자르고_잘린_쪽에_말줄임표를_붙인다() {
        String text = "가".repeat(100) + "단서" + "나".repeat(100);
        String clipped = PassageFinder.clip(text, List.of("단서"), 30);
        assertThat(clipped).startsWith("…").endsWith("…").contains("단서");
        assertThat(clipped.codePointCount(0, clipped.length())).isLessThanOrEqualTo(32);

        assertThat(PassageFinder.clip("짧은 글", List.of("짧은"), 30)).isEqualTo("짧은 글");
        // 맨 앞에서 맞으면 앞에는 붙이지 않는다
        assertThat(PassageFinder.clip("단서" + "다".repeat(50), List.of("단서"), 10)).startsWith("단서").endsWith("…");
        // 맨 끝에서 맞아도 길이를 채워 자르고 뒤에는 붙이지 않는다
        String tail = PassageFinder.clip("라".repeat(50) + "단서", List.of("단서"), 10);
        assertThat(tail).startsWith("…").endsWith("단서");
        // 소문자로 바꾸면 길어지는 글자(İ → i̇)가 앞에 있어도 맞은 곳을 놓치지 않는다
        assertThat(PassageFinder.clip("İ".repeat(100) + "단서" + "가".repeat(100), List.of("단서"), 30)).contains("단서");
        // 이모지(서로게이트 쌍)를 반으로 자르지 않는다
        String emoji = PassageFinder.clip("😀".repeat(40) + "단서", List.of("단서"), 10);
        // 홀로 남은 서로게이트는 codePoints에 그 값 그대로 나온다
        assertThat(emoji.codePoints().filter(c -> c >= 0xD800 && c <= 0xDFFF)).isEmpty();
        assertThat(emoji).endsWith("단서");
    }
}
