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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 검색어 규칙과 특수문자 (specs/004 US6, quickstart S-6, S-9의 1, T032, T040).
 * 2자 미만·50자 초과는 쿼리 전에 400, 특수문자는 일반 글자로 찾고 오류가 아니다 (FR-010, FR-018, SC-008, D-1: A).
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostSearchKeywordTest {

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
    private String tag;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = members.register("a");
        tag = UUID.randomUUID().toString().substring(0, 6);
    }

    @Test
    void S6_1_2_3_4_2자_미만은_400이고_찾지_않는다() throws Exception {
        mvc.perform(get("/api/search/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_KEYWORD_TOO_SHORT"))
                .andExpect(jsonPath("$.message").value("검색어를 2자 이상 입력해 주세요"));
        for (String q : new String[] {"", "   ", "가", " 가 ", "　가　"}) {
            mvc.perform(get("/api/search/posts").param("q", q))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SEARCH_KEYWORD_TOO_SHORT"));
        }
    }

    @Test
    void S6_10_50자를_넘으면_400이고_50자는_된다() throws Exception {
        mvc.perform(get("/api/search/posts").param("q", "가".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_KEYWORD_TOO_LONG"))
                .andExpect(jsonPath("$.message").value("검색어는 50자까지 입력할 수 있습니다"));
        mvc.perform(get("/api/search/posts").param("q", "  " + "가".repeat(50) + "  ")).andExpect(status().isOk());
        mvc.perform(get("/api/search/posts").param("q", "😀".repeat(50))).andExpect(status().isOk());
    }

    @Test
    void S6_5_앞뒤_공백은_지우고_찾는다() throws Exception {
        Long id = posts.add(a.defaultCategoryId(), "단풍" + tag, "본문", "public", BASE);
        mvc.perform(get("/api/search/posts").param("q", "  단풍" + tag + "  "))
                .andExpect(jsonPath("$.keyword").value("단풍" + tag))
                .andExpect(jsonPath("$.results[0].postId").value(id));
    }

    @Test
    void S6_7_8_퍼센트와_밑줄은_일반_글자다() throws Exception {
        Long percent = posts.add(a.defaultCategoryId(), "할인 100% " + tag, "본문", "public", BASE);
        posts.add(a.defaultCategoryId(), "할인 1000원 " + tag, "본문", "public", BASE);
        Long underscore = posts.add(a.defaultCategoryId(), "a_b " + tag, "본문", "public", BASE);
        posts.add(a.defaultCategoryId(), "axb " + tag, "본문", "public", BASE);
        assertThat(ids("100% " + tag)).containsExactly(percent);
        assertThat(ids("a_b " + tag)).containsExactly(underscore);
    }

    @Test
    void S6_9_S9_1_특수문자와_SQL_모양은_글자_그대로_찾고_오류가_아니다() throws Exception {
        Long quoted = posts.add(a.defaultCategoryId(), tag + " ' OR 1=1 --", "본문", "public", BASE);
        posts.add(a.defaultCategoryId(), tag + " 비밀", "본문", "private", BASE);
        for (String q : new String[] {"\\\\", "''", "\"\"", ";;", "--", "<script>", "((", "))",
                "' OR 1=1 --", "'; DROP TABLE post; --", "\\%_"}) {
            String body = mvc.perform(get("/api/search/posts").param("q", q))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(body).doesNotContain("Exception").doesNotContain("select ");
        }
        assertThat(ids(tag + " ' OR 1=1 --")).containsExactly(quoted);
        assertThat(jdbc.queryForObject("select count(*) from post where title like ?", Long.class, tag + "%"))
                .isEqualTo(2);
    }

    private List<Long> ids(String q) throws Exception {
        String body = mvc.perform(get("/api/search/posts").param("q", q)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> ids = JsonPath.read(body, "$.results[*].postId");
        return ids.stream().map(Number::longValue).toList();
    }
}
