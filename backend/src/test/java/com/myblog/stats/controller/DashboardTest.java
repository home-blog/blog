package com.myblog.stats.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestStats;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
 * 대시보드 (specs/006 US7, T044, quickstart S-9의 1 ~ 6, FR-006 ~ FR-011, SC-008, SC-009).
 * 숫자 줄은 통계 표에 바로 넣는다 (실제로 쌓이는 것은 Phase 11). 지금 시각은 한국 2026-10-08 오후 3시.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestStats.class, TestClock.Config.class})
class DashboardTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestStats stats;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(NOW);
        owner = members.register("o");
        session = members.login(mvc, owner);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void S9_1_새_블로그는_숫자가_모두_0이고_두_목록은_비어_있다() throws Exception {
        mvc.perform(get("/api/manage/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views.today").value(0))
                .andExpect(jsonPath("$.views.yesterday").value(0))
                .andExpect(jsonPath("$.views.total").value(0))
                .andExpect(jsonPath("$.visitors.total").value(0))
                .andExpect(jsonPath("$.newCommentCount").value(0))
                .andExpect(jsonPath("$.popularPosts", hasSize(0)))
                .andExpect(jsonPath("$.recentPosts", hasSize(0)));
    }

    @Test
    void S9_2_3_오늘_어제는_통계의_같은_날_숫자와_같고_누적은_모든_줄의_합이고_그래프는_30일() throws Exception {
        stats.addBlogDay(owner.blogId(), TODAY, 12, 8);
        stats.addBlogDay(owner.blogId(), TODAY.minusDays(1), 32, 21);
        stats.addBlogDay(owner.blogId(), TODAY.minusDays(100), 1000, 500);
        String dashboard = mvc.perform(get("/api/manage/dashboard").session(session))
                .andExpect(jsonPath("$.views.today").value(12))
                .andExpect(jsonPath("$.views.yesterday").value(32))
                .andExpect(jsonPath("$.views.total").value(1044))
                .andExpect(jsonPath("$.visitors.today").value(8))
                .andExpect(jsonPath("$.visitors.yesterday").value(21))
                .andExpect(jsonPath("$.visitors.total").value(529))
                .andExpect(jsonPath("$.chart", hasSize(30)))
                .andExpect(jsonPath("$.chart[29].date").value("2026-10-08"))
                .andExpect(jsonPath("$.chart[29].views").value(12))
                .andExpect(jsonPath("$.chart[28].visitors").value(21))
                .andReturn().getResponse().getContentAsString();
        String daily = mvc.perform(get("/api/manage/stats").param("days", "7").session(session))
                .andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(daily, "$.daily[6].views")).isEqualTo(JsonPath.read(dashboard, "$.views.today"));
        assertThat((Integer) JsonPath.read(daily, "$.daily[5].visitors"))
                .isEqualTo(JsonPath.read(dashboard, "$.visitors.yesterday"));
    }

    @Test
    void S9_2_글을_지워도_누적은_줄지_않는다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "지울 글", "public");
        stats.addBlogDay(owner.blogId(), TODAY, 5, 1);
        stats.addPostDay(postId, TODAY, 5);
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/manage/dashboard").session(session))
                .andExpect(jsonPath("$.views.total").value(5))
                .andExpect(jsonPath("$.popularPosts", hasSize(0)));
    }

    @Test
    void S9_4_인기_글은_최근_7일_조회수_순_공개_글만_5개_같으면_늦게_쓴_글_먼저() throws Exception {
        Instant base = Instant.parse("2026-09-01T00:00:00Z");
        Long secretCategory = members.addCategory(owner.blogId(), "비밀", "private", 2);
        Long top = post("최다", "public", base);
        Long privatePost = post("비공개 최다", "private", base.plusSeconds(1));
        Long inSecretCategory = members.addPost(secretCategory, "비공개 분류의 공개 글", "public", base.plusSeconds(2));
        Long olderTie = post("같은 수 먼저 쓴 글", "public", base.plusSeconds(3));
        Long newerTie = post("같은 수 나중 쓴 글", "public", base.plusSeconds(4));
        Long third = post("셋째", "public", base.plusSeconds(5));
        Long fourth = post("넷째", "public", base.plusSeconds(6));
        Long fifth = post("다섯째", "public", base.plusSeconds(7));
        Long sixth = post("여섯째", "public", base.plusSeconds(8));
        Long oldOnly = post("8일 전에만 많이 읽힌 글", "public", base.plusSeconds(9));
        stats.addPostDay(top, TODAY, 50);
        stats.addPostDay(top, TODAY.minusDays(6), 50);         // 7일 안 (오늘 포함)
        stats.addPostDay(privatePost, TODAY, 500);
        stats.addPostDay(inSecretCategory, TODAY, 400);
        stats.addPostDay(olderTie, TODAY, 30);
        stats.addPostDay(newerTie, TODAY.minusDays(1), 30);
        stats.addPostDay(third, TODAY, 20);
        stats.addPostDay(fourth, TODAY, 10);
        stats.addPostDay(fifth, TODAY, 5);
        stats.addPostDay(sixth, TODAY, 1);
        stats.addPostDay(oldOnly, TODAY.minusDays(7), 900);    // 8일 전: 세지 않는다

        String body = mvc.perform(get("/api/manage/dashboard").session(session))
                .andExpect(jsonPath("$.popularPosts", hasSize(5)))
                .andExpect(jsonPath("$.popularPosts[0].title").value("최다"))
                .andExpect(jsonPath("$.popularPosts[0].views").value(100))
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.popularPosts[*].postId");
        assertThat(ids.stream().map(Number::longValue).toList()).containsExactly(top, newerTie, olderTie, third, fourth);

        // 가장 많이 읽힌 글을 비공개로 바꾸면 빠진다
        jdbc.update("update post set visibility = 'private' where post_id = ?", top);
        entityManager.clear(); // 테스트 트랜잭션 하나에서 앞 요청이 읽어 둔 글을 버린다
        String after = mvc.perform(get("/api/manage/dashboard").session(session)).andReturn().getResponse().getContentAsString();
        List<Number> afterIds = JsonPath.read(after, "$.popularPosts[*].postId");
        assertThat(afterIds.stream().map(Number::longValue).toList()).containsExactly(newerTie, olderTie, third, fourth, fifth);
    }

    @Test
    void S9_5_최근_글은_비공개_포함_5개이고_공개_여부가_있다() throws Exception {
        Instant base = Instant.parse("2026-09-01T00:00:00Z");
        for (int i = 0; i < 6; i++) {
            post("글" + i, i == 5 ? "private" : "public", base.plusSeconds(i * 60L));
        }
        mvc.perform(get("/api/manage/dashboard").session(session))
                .andExpect(jsonPath("$.recentPosts", hasSize(5)))
                .andExpect(jsonPath("$.recentPosts[0].title").value("글5"))
                .andExpect(jsonPath("$.recentPosts[0].visibility").value("private"))
                .andExpect(jsonPath("$.recentPosts[0].createdAt").value("2026-09-01T00:05:00Z"))
                .andExpect(jsonPath("$.recentPosts[4].title").value("글1"));
    }

    @Test
    void S9_6_새_댓글_수는_new_count와_같다() throws Exception {
        Long postId = post("글", "public", NOW.minusSeconds(3600));
        Member reader = members.register("r");
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '댓글', now()), (?, ?, '댓글2', now())",
                reader.id(), postId, owner.id(), postId);
        mvc.perform(get("/api/manage/dashboard").session(session)).andExpect(jsonPath("$.newCommentCount").value(1));
        mvc.perform(get("/api/manage/comments/new-count").session(session)).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void 로그인하지_않으면_401이고_회원_B는_B의_숫자만() throws Exception {
        mvc.perform(get("/api/manage/dashboard")).andExpect(status().isUnauthorized());
        stats.addBlogDay(owner.blogId(), TODAY, 12, 8);
        Member b = members.register("b");
        MockHttpSession bSession = members.login(mvc, b);
        mvc.perform(get("/api/manage/dashboard").session(bSession))
                .andExpect(jsonPath("$.views.today").value(0))
                .andExpect(jsonPath("$.views.total").value(0));
    }

    private Long post(String title, String visibility, Instant createdAt) {
        return members.addPost(owner.defaultCategoryId(), title, visibility, createdAt);
    }
}
