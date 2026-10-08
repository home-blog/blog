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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 목록을 최신순으로 10개씩 본다 (specs/004 US1, quickstart S-1, T014, T037).
 * 공개 글 29개 = 차례로 쓴 25개 + 같은 시각의 T1·T2 + 긴 글 + 짧은 글. 실제 PostgreSQL로 확인한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostListTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;
    private Member owner;
    private Long t1;
    private Long t2;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        Long category = owner.defaultCategoryId();
        for (int i = 1; i <= 25; i++) {
            posts.add(category, "글 " + i, "본문 " + i, "public", BASE.plus(i, ChronoUnit.HOURS));
        }
        Instant same = BASE.plus(10, ChronoUnit.MINUTES);
        t1 = posts.add(category, "T1", "같은 시각 먼저", "public", same);
        t2 = posts.add(category, "T2", "같은 시각 나중", "public", same);
        String longBody = ("가".repeat(50) + "\n").repeat(3);
        posts.add(category, "긴 글", longBody, "public", BASE.plus(30, ChronoUnit.HOURS));
        posts.add(category, "짧은 글", "나".repeat(80), "public", BASE.plus(29, ChronoUnit.HOURS));
    }

    @Test
    void S1_1_2_최신_글이_맨_위이고_29개의_글_첫_페이지는_10개() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.categoryId").doesNotExist())
                .andExpect(jsonPath("$.totalCount").value(29))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.posts.length()").value(10))
                .andExpect(jsonPath("$.posts[0].title").value("긴 글"))
                .andExpect(jsonPath("$.posts[1].title").value("짧은 글"))
                .andExpect(jsonPath("$.posts[2].title").value("글 25"))
                .andExpect(jsonPath("$.posts[0].categoryName").value("미분류"))
                .andExpect(jsonPath("$.posts[0].categoryId").value(owner.defaultCategoryId()))
                .andExpect(jsonPath("$.posts[0].visibility").value("public"))
                .andExpect(jsonPath("$.posts[0].createdAt").value("2026-01-02T06:00:00Z"));
    }

    @Test
    void S1_4_5_6_모든_페이지에서_위_글이_같거나_최신이고_같은_시각이면_나중_글이_위() throws Exception {
        List<String> times = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            String body = read("/api/blogs/" + owner.blogId() + "/posts?page=" + page);
            List<String> pageTimes = JsonPath.read(body, "$.posts[*].createdAt");
            List<Number> pageIds = JsonPath.read(body, "$.posts[*].postId");
            assertThat(pageTimes).hasSizeLessThanOrEqualTo(10);
            assertThat(pageTimes).hasSize(page == 3 ? 9 : 10);
            times.addAll(pageTimes);
            pageIds.forEach(id -> ids.add(id.longValue()));
        }
        assertThat(times).hasSize(29);
        for (int i = 1; i < times.size(); i++) {
            assertThat(Instant.parse(times.get(i - 1))).isAfterOrEqualTo(Instant.parse(times.get(i)));
        }
        assertThat(ids.indexOf(t2)).isEqualTo(ids.indexOf(t1) - 1);
    }

    @Test
    void S1_7_너무_큰_페이지는_마지막_페이지() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.posts.length()").value(9));
    }

    @Test
    void S9_2_1보다_작거나_숫자가_아닌_페이지는_1페이지이고_오류가_아니다() throws Exception {
        for (String page : new String[] {"0", "-1", "abc", "1 OR 1=1", "99999999999999999999"}) {
            mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("page", page))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(page.startsWith("9") ? 3 : 1));
        }
    }

    @Test
    void S1_8_9_긴_본문은_한_줄_100자와_말줄임표_짧은_본문은_그대로() throws Exception {
        String body = read("/api/blogs/" + owner.blogId() + "/posts");
        String longPreview = JsonPath.read(body, "$.posts[0].preview");
        assertThat(longPreview).doesNotContain("\n").endsWith("…");
        assertThat(longPreview.codePointCount(0, longPreview.length())).isEqualTo(101);
        assertThat(longPreview).startsWith("가".repeat(50) + " 가");
        assertThat((String) JsonPath.read(body, "$.posts[1].preview")).isEqualTo("나".repeat(80));
    }

    @Test
    void 글이_0개면_1페이지뿐이고_오류가_아니다() throws Exception {
        Member empty = members.register("e");
        mvc.perform(get("/api/blogs/{id}/posts", empty.blogId()).param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void 없는_블로그는_404() throws Exception {
        mvc.perform(get("/api/blogs/999999999/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
        mvc.perform(get("/api/blogs/abc/posts")).andExpect(status().isNotFound());
    }

    private String read(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
