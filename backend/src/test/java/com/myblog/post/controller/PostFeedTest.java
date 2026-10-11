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
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * 첫 화면 글 모음 (GET /api/posts, /api/posts/popular): 여러 블로그의 공개 글을 최신순·주제별로, 이번 주 인기 글.
 * 글 모음은 서비스 전체를 보여 주므로, 다른 테스트·개발 데이터보다 늘 위에 오도록 먼 미래 시각으로 글을 만든다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostFeedTest {

    /** 다른 데이터보다 늘 최신이 되도록 먼 미래 */
    private static final Instant FUTURE = Instant.parse("2100-01-01T00:00:00Z");

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
    private Long travelA;
    private Long sportB;
    private Long travelB;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = members.register("feeda");
        b = members.register("feedb");
        travelA = add(a.defaultCategoryId(), "여행", "A의 여행 글", "public", 1);
        sportB = add(b.defaultCategoryId(), "운동", "B의 운동 글", "public", 2);
        travelB = add(b.defaultCategoryId(), "여행", "B의 여행 글", "public", 3);
    }

    @Test
    void 여러_블로그의_공개_글을_최신순으로_블로그_이름과_글쓴이를_붙여_보여_준다() throws Exception {
        String body = mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").doesNotExist())
                .andExpect(jsonPath("$.topics.length()").value(5))
                .andExpect(jsonPath("$.topics[0]").value("여행"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(travelB))
                .andExpect(jsonPath("$.posts[0].blogId").value(b.blogId()))
                .andExpect(jsonPath("$.posts[0].nickname").value(b.user().getNickname()))
                .andExpect(jsonPath("$.posts[0].blogName").value(b.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.posts[0].topic").value("여행"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(ids(body).subList(0, 3)).containsExactly(travelB, sportB, travelA);
    }

    @Test
    void 주제를_고르면_그_주제의_글만() throws Exception {
        String body = mvc.perform(get("/api/posts").param("topic", "여행"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("여행"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(ids(body)).contains(travelB, travelA).doesNotContain(sportB);
        List<String> topics = JsonPath.read(body, "$.posts[*].topic");
        assertThat(topics).containsOnly("여행");
    }

    @Test
    void 없는_주제는_400() throws Exception {
        mvc.perform(get("/api/posts").param("topic", "없는주제"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
        mvc.perform(get("/api/posts/popular").param("topic", "없는주제"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
    }

    @Test
    void 비공개_글과_비공개_분류의_글은_나오지_않는다() throws Exception {
        Long secret = add(a.defaultCategoryId(), "여행", "비공개 글", "private", 10);
        Long hiddenCategory = jdbc.queryForObject("insert into category (blog_id, name, visibility, is_default, sort_order,"
                + " created_at) values (?, '숨김', 'private', false, 9, now()) returning category_id", Long.class,
                a.blogId());
        Long inHidden = add(hiddenCategory, "여행", "숨긴 분류의 글", "public", 11);

        String body = mvc.perform(get("/api/posts")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(ids(body)).doesNotContain(secret, inHidden).startsWith(travelB);
    }

    @Test
    void 댓글_수와_좋아요_수를_함께_준다() throws Exception {
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '좋아요!', now())",
                a.user().getId(), travelB);
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '저도요', now())",
                b.user().getId(), travelB);
        jdbc.update("insert into post_like (post_id, users_id, created_at) values (?, ?, now())", travelB,
                a.user().getId());

        mvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.posts[0].postId").value(travelB))
                .andExpect(jsonPath("$.posts[0].commentCount").value(2))
                .andExpect(jsonPath("$.posts[0].likeCount").value(1))
                .andExpect(jsonPath("$.posts[1].commentCount").value(0));
    }

    @Test
    void 페이지는_12개씩이고_범위를_넘으면_마지막_페이지() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.pageSize").value(12));
        mvc.perform(get("/api/posts").param("page", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(org.hamcrest.Matchers.lessThan(99999)));
    }

    @Test
    void 인기_글은_최근_7일_조회수가_많은_순이고_오래된_조회수는_세지_않는다() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        stat(sportB, today, 900_000);
        stat(travelA, today.minusDays(6), 800_000);
        stat(travelB, today.minusDays(7), 5_000_000); // 기간 밖

        String body = mvc.perform(get("/api/posts/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Long> popular = ids(body);
        // 기간 밖 조회수(500만)는 세지 않으므로 travelB는 최근 조회수가 있는 두 글보다 앞에 올 수 없다
        assertThat(popular).hasSizeLessThanOrEqualTo(5).startsWith(sportB, travelA);

        String travel = mvc.perform(get("/api/posts/popular").param("topic", "여행"))
                .andExpect(jsonPath("$.topic").value("여행"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(ids(travel)).startsWith(travelA).doesNotContain(sportB);
    }

    @Test
    void 로그인하지_않아도_볼_수_있다() throws Exception {
        mvc.perform(get("/api/posts")).andExpect(status().isOk());
        mvc.perform(get("/api/posts/popular")).andExpect(status().isOk());
    }

    private Long add(Long categoryId, String topic, String title, String visibility, int hours) {
        Long id = posts.add(categoryId, title, "본문 " + title, visibility, FUTURE.plus(hours, ChronoUnit.HOURS));
        jdbc.update("update post set topic_id = (select topic_id from topic where topic_name = ?) where post_id = ?",
                topic, id);
        return id;
    }

    private void stat(Long postId, LocalDate day, int views) {
        jdbc.update("insert into post_daily_stat (post_id, stat_date, views) values (?, ?, ?)", postId, Date.valueOf(day),
                views);
    }

    private static List<Long> ids(String body) {
        List<Number> raw = JsonPath.read(body, "$.posts[*].postId");
        return raw.stream().map(Number::longValue).toList();
    }
}
