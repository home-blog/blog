package com.myblog.stats.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestStats;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 모든 관리 주소의 주인 확인과 입력 방어 (specs/006 T059, quickstart S-1의 5·6, S-11의 1·2·4, FR-002, FR-003, SC-001,
 * NF-02, NF-06, NF-07, NF-11). 회원 B는 B의 것만 받고, A의 번호로 보낸 변경은 404이고 A의 데이터는 그대로다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestStats.class})
class ManageAccessTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestStats stats;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member a;
    private Member b;
    private MockHttpSession bSession;
    private Long aCategory;
    private Long aPost;
    private Long aComment;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = members.register("a");
        b = members.register("b");
        bSession = members.login(mvc, b);
        aCategory = members.addCategory(a.blogId(), "A의 분류", "public", 2);
        aPost = members.addPost(aCategory, "A의 비공개 글", "private");
        aComment = jdbc.queryForObject("insert into comment (users_id, post_id, body, created_at) values (?, ?, 'A 글의 댓글', now())"
                + " returning comment_id", Long.class, a.id(), aPost);
        stats.addBlogDay(a.blogId(), LocalDate.now(ZoneId.of("Asia/Seoul")), 77, 55);
    }

    @Test
    void S1_5_회원_B는_대시보드_글_댓글_통계_모두_B의_것만_받는다() throws Exception {
        mvc.perform(get("/api/manage/blog").session(bSession)).andExpect(jsonPath("$.blogId").value(b.blogId()));
        mvc.perform(get("/api/manage/dashboard").session(bSession))
                .andExpect(jsonPath("$.views.total").value(0))
                .andExpect(jsonPath("$.recentPosts.length()").value(0));
        mvc.perform(get("/api/manage/posts").session(bSession)).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/manage/comments").session(bSession)).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/manage/comments/new-count").session(bSession)).andExpect(jsonPath("$.count").value(0));
        String daily = mvc.perform(get("/api/manage/stats").param("days", "7").session(bSession))
                .andReturn().getResponse().getContentAsString();
        assertThat(daily).doesNotContain("77", "55");
    }

    @Test
    void S1_6_A의_번호로_보낸_거르기_분류_고치기_순서_삭제는_404이고_A의_데이터는_그대로() throws Exception {
        mvc.perform(get("/api/manage/posts").param("categoryId", aCategory.toString()).session(bSession))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/me/blog/categories/{id}", aCategory).with(csrf()).session(bSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "빼앗기")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/me/blog/categories/{id}", aCategory).with(csrf()).session(bSession))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/me/blog/categories/order").with(csrf()).session(bSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryIds\":[" + aCategory + "," + b.defaultCategoryId() + "]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/posts/{id}", aPost).with(csrf()).session(bSession)).andExpect(status().isNotFound());
        // 댓글 삭제는 005의 약속대로 없는 댓글과 같은 404 (2026-10-08 결정: 403을 쓰지 않음)
        mvc.perform(delete("/api/comments/{id}", aComment).with(csrf()).session(bSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        assertThat(jdbc.queryForObject("select name from category where category_id = ?", String.class, aCategory))
                .isEqualTo("A의 분류");
        assertThat(jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, aPost)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from comment where comment_id = ?", Long.class, aComment)).isOne();
    }

    @Test
    void S11_1_CSRF_토큰_없이_읽음_처리_설정_저장_분류_추가는_거절() throws Exception {
        mvc.perform(post("/api/manage/comments/read").session(bSession)).andExpect(status().isForbidden());
        mvc.perform(put("/api/me/blog").session(bSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("name", "바뀜", "intro", "")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/me/blog/categories").session(bSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("name", "새 분류")))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select comments_read_at from blog where blog_id = ?", Object.class, b.blogId())).isNull();
        assertThat(jdbc.queryForObject("select name from blog where blog_id = ?", String.class, b.blogId())).isNotEqualTo("바뀜");
    }

    @Test
    void S11_2_거르기와_기간에_SQL을_넣어도_400이고_DB는_그대로() throws Exception {
        String attack = "' OR 1=1 --";
        mvc.perform(get("/api/manage/posts").param("visibility", attack).session(bSession)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/manage/posts").param("categoryId", attack).session(bSession)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/manage/stats").param("days", attack).session(bSession)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/manage/comments").param("newSince", attack).session(bSession)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, aPost)).isOne();
    }

    @Test
    void S11_4_잘못된_JSON과_없는_주소는_내부_정보가_없다() throws Exception {
        String badJson = mvc.perform(put("/api/me/blog").with(csrf()).session(bSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/manage/nothing").session(bSession))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        for (String body : new String[] {badJson, missing}) {
            assertThat(body).doesNotContain("Exception", "com.myblog", "select ", "/api/manage/nothing", "at org.");
        }
    }

    @Test
    void 로그인하지_않으면_모든_관리_주소가_401() throws Exception {
        for (String path : new String[] {"/api/manage/blog", "/api/manage/dashboard", "/api/manage/posts", "/api/manage/comments",
                "/api/manage/comments/new-count", "/api/manage/stats"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }
}
