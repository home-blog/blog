package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 여러 블로그의 공개 글을 키워드로 찾는다 (specs/004 US5, quickstart S-5, S-2a의 5·7, T026, T039).
 * 검색은 서비스 전체를 찾으므로, 다른 테스트·개발 데이터와 섞이지 않게 단어마다 이번 실행만의 꼬리표를 붙인다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostSearchTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member a;
    private Member b;
    private String tag;
    private String maple;
    private String spot;
    private Long k1;
    private Long k2;
    private Long k3;
    private Long k5;
    private Long k6;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tag = UUID.randomUUID().toString().substring(0, 6);
        maple = "단풍" + tag;
        spot = "명소" + tag;
        a = members.register("a");
        b = members.register("b");
        Long cat = a.defaultCategoryId();
        k1 = posts.add(cat, "올해 " + maple + " 여행", "본문", "public", BASE.plus(1, ChronoUnit.HOURS));
        k2 = posts.add(cat, "가을 이야기", "산에 " + maple + "이 들었다", "public", BASE.plus(2, ChronoUnit.HOURS));
        k3 = posts.add(cat, maple + " 구경", "가 볼 만한 " + spot + " 세 곳", "public", BASE.plus(3, ChronoUnit.HOURS));
        posts.add(cat, spot + " 추천", "본문", "public", BASE.plus(4, ChronoUnit.HOURS));
        k5 = posts.add(cat, "Spring Boot " + tag, "본문", "public", BASE.plus(5, ChronoUnit.HOURS));
        k6 = posts.add(b.defaultCategoryId(), "B의 " + maple, "본문", "public", BASE.plus(6, ChronoUnit.HOURS));
        posts.add(cat, "비밀단어" + tag + " 1", "본문", "private", BASE);
        posts.add(cat, "비밀단어" + tag + " 2", "본문", "private", BASE);
    }

    @Test
    void S5_1_한_단어는_제목이나_본문에_있는_글을_블로그가_달라도_찾는다() throws Exception {
        assertThat(ids(search(maple))).containsExactly(k6, k3, k2, k1);
    }

    @Test
    void S5_2_여러_단어는_모두_들어_있는_글만() throws Exception {
        assertThat(ids(search(maple + " " + spot))).containsExactly(k3);
    }

    @Test
    void S5_3_대소문자를_가리지_않는다() throws Exception {
        assertThat(ids(search("spring boot " + tag))).containsExactly(k5);
        assertThat(ids(search("SPRING BOOT " + tag.toUpperCase()))).containsExactly(k5);
    }

    @Test
    void S5_4_5_비공개_글은_로그아웃이든_주인이든_나오지_않는다() throws Exception {
        MockHttpSession owner = members.login(mvc, a);
        for (MockHttpSession session : new MockHttpSession[] {new MockHttpSession(), owner}) {
            mvc.perform(get("/api/search/posts").param("q", "비밀단어" + tag).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.results.length()").value(0));
        }
    }

    @Test
    void S5_6_15건이면_10개와_5개이고_최신이_위() throws Exception {
        String word = "열다섯" + tag;
        for (int i = 1; i <= 15; i++) {
            posts.add(a.defaultCategoryId(), word + " " + i, "본문", "public", BASE.plus(i, ChronoUnit.DAYS));
        }
        mvc.perform(get("/api/search/posts").param("q", word))
                .andExpect(jsonPath("$.totalCount").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.results.length()").value(10))
                .andExpect(jsonPath("$.results[0].title").value(word + " 15"));
        mvc.perform(get("/api/search/posts").param("q", word).param("page", "2"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.results.length()").value(5))
                .andExpect(jsonPath("$.results[4].title").value(word + " 1"));
        mvc.perform(get("/api/search/posts").param("q", word).param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
        mvc.perform(get("/api/search/posts").param("q", word).param("page", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void S5_7_한_줄에_블로그_이름_분류_작성일_제목_미리보기가_있고_공개_여부는_없다() throws Exception {
        String longBody = maple + " " + "가".repeat(150);
        posts.add(a.defaultCategoryId(), "긴 글", longBody, "public", BASE.plus(10, ChronoUnit.DAYS));
        mvc.perform(get("/api/search/posts").param("q", maple))
                .andExpect(jsonPath("$.keyword").value(maple))
                .andExpect(jsonPath("$.results[0].blogId").value(a.blogId()))
                .andExpect(jsonPath("$.results[0].blogName").value(a.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.results[0].categoryId").value(a.defaultCategoryId()))
                .andExpect(jsonPath("$.results[0].categoryName").value("미분류"))
                .andExpect(jsonPath("$.results[0].createdAt").value("2026-01-11T00:00:00Z"))
                .andExpect(jsonPath("$.results[0].title").value("긴 글"))
                .andExpect(jsonPath("$.results[0].visibility").doesNotExist());
        String preview = JsonPath.read(read(maple), "$.results[0].preview");
        // 검색 미리보기도 글 목록과 같은 100자 (D-3: A)
        assertThat(preview.codePointCount(0, preview.length())).isEqualTo(101);
        assertThat(preview).endsWith("…");
    }

    @Test
    void S5_8_없는_단어는_200과_0건() throws Exception {
        mvc.perform(get("/api/search/posts").param("q", "없는단어조합xyz" + tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void S2a_5_7_비공개_분류의_공개_글은_주인이어도_검색에_없고_공개로_바꾸면_나온다() throws Exception {
        Member d = members.register("d");
        Long diary = members.addCategory(d.blogId(), "일기", "private", 1);
        String hidden = "숨김단어" + tag;
        posts.add(diary, hidden + " 1", "본문", "public", BASE);
        posts.add(diary, hidden + " 2", "본문", "public", BASE.plusSeconds(1));
        for (MockHttpSession session : new MockHttpSession[] {new MockHttpSession(), members.login(mvc, d)}) {
            mvc.perform(get("/api/search/posts").param("q", hidden).session(session))
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
        jdbc.update("update category set visibility = 'public' where category_id = ?", diary);
        mvc.perform(get("/api/search/posts").param("q", hidden))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.results[0].categoryName").value("일기"));
    }

    private String search(String q) throws Exception {
        return read(q);
    }

    private String read(String q) throws Exception {
        return mvc.perform(get("/api/search/posts").param("q", q)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Long> ids(String body) {
        List<Number> ids = JsonPath.read(body, "$.results[*].postId");
        return ids.stream().map(Number::longValue).toList();
    }
}
