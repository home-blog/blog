package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
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
 * 오늘의 이슈 (specs/015 US2, S2-1 ~ S2-3): 오늘과 어제(한국 날짜) 읽힌 수의 합이 큰 공개 글, 블로그마다 하나, 3개.
 * 시계를 먼 미래의 서로 다른 날로 옮겨, 그날의 일별 통계에는 이 테스트가 넣은 줄만 있게 한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class, TestClock.Config.class})
class TodayIssueTest {

    private static final String URL = "/api/v1/posts/today-issue";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Instant now;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // 2100년대의 아무 날 낮 12시(한국): 테스트마다 다른 날이라 앞 테스트의 보관 값과 섞이지 않는다
        today = LocalDate.of(2100, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1, 300_000));
        now = today.atTime(12, 0).atZone(SEOUL).toInstant();
        clock.set(now);
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    private Long post(Member member, String title, String visibility) {
        return posts.add(member.defaultCategoryId(), title, "본문 " + title, visibility, now.minus(Duration.ofDays(3)));
    }

    private void views(Long postId, LocalDate day, int views) {
        jdbc.update("insert into post_daily_stat (post_id, stat_date, views) values (?, ?, ?)", postId,
                Date.valueOf(day), views);
    }

    private List<Long> issueIds() throws Exception {
        String body = mvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> raw = JsonPath.read(body, "$.posts[*].postId");
        return raw.stream().map(Number::longValue).toList();
    }

    @Test
    void 오늘과_어제_읽힌_수의_합이_큰_순이고_그_밖의_날은_세지_않는다() throws Exception {
        Member a = members.register("issa");
        Member b = members.register("issb");
        Member c = members.register("issc");
        Long first = post(a, "어제와 오늘 합 1위", "public");
        Long second = post(b, "오늘만 2위", "public");
        Long old = post(c, "그제만 많이 읽힘", "public");
        views(first, today, 40);
        views(first, today.minusDays(1), 70);
        views(second, today, 100);
        views(old, today.minusDays(2), 9_999);

        assertThat(issueIds()).containsExactly(first, second);
        mvc.perform(get(URL))
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.posts[0].blogName").isNotEmpty())
                .andExpect(jsonPath("$.posts[0].title").value("어제와 오늘 합 1위"));
    }

    @Test
    void S2_1_볼_수_없는_글은_넣지_않는다() throws Exception {
        Member a = members.register("issa");
        Member b = members.register("issb");
        Member gone = members.register("issg");
        Long hidden = post(a, "비공개 글", "private");
        Long closedCategory = members.addPost(members.addCategory(b.blogId(), "숨긴 분류", "private", 9), "비공개 분류의 글",
                "public");
        Long withdrawn = post(gone, "탈퇴한 회원의 글", "public");
        jdbc.update("update users set deleted_at = now() where users_id = ?", gone.id());
        Long open = post(a, "공개 글", "public");
        views(hidden, today, 900);
        views(closedCategory, today, 800);
        views(withdrawn, today, 700);
        views(open, today, 1);

        assertThat(issueIds()).containsExactly(open);
    }

    @Test
    void S2_2_세_개까지이고_한_블로그에서는_하나만() throws Exception {
        Member a = members.register("issa");
        Member b = members.register("issb");
        Member c = members.register("issc");
        Member d = members.register("issd");
        Long a1 = post(a, "A 1", "public");
        Long a2 = post(a, "A 2", "public");
        Long b1 = post(b, "B 1", "public");
        Long c1 = post(c, "C 1", "public");
        Long d1 = post(d, "D 1", "public");
        views(a1, today, 500);
        views(a2, today, 400);
        views(b1, today, 300);
        views(c1, today, 200);
        views(d1, today, 100);

        assertThat(issueIds()).containsExactly(a1, b1, c1);
    }

    @Test
    void 읽힌_글이_없으면_빈_목록() throws Exception {
        Member a = members.register("issa");
        Long unread = post(a, "아무도 안 읽음", "public");
        views(unread, today, 0);

        assertThat(issueIds()).isEmpty();
    }

    @Test
    void S2_3_몇_분_동안은_같은_순서를_보관하되_볼_수_없게_된_글은_바로_뺀다() throws Exception {
        Member a = members.register("issa");
        Member b = members.register("issb");
        Long first = post(a, "먼저 1위", "public");
        Long later = post(b, "나중에 많이 읽힘", "public");
        views(first, today, 10);
        assertThat(issueIds()).containsExactly(first);

        views(later, today, 1_000);
        assertThat(issueIds()).as("보관하는 동안은 같은 순서").containsExactly(first);

        clock.set(now.plus(Duration.ofMinutes(6)));
        assertThat(issueIds()).as("보관 시간이 지나면 다시 센다").containsExactly(later, first);

        jdbc.update("update post set visibility = 'private' where post_id = ?", later);
        assertThat(issueIds()).as("보관 중이어도 비공개로 바뀐 글은 바로 빠진다").containsExactly(first);
    }

    @Test
    void 날짜가_바뀌면_보관_시간_안이어도_다시_센다() throws Exception {
        clock.set(today.atTime(23, 58).atZone(SEOUL).toInstant());
        Member a = members.register("issa");
        Member b = members.register("issb");
        Long yesterdayOnly = post(a, "어제 읽힘", "public");
        Long tomorrow = post(b, "내일 읽힘", "public");
        views(yesterdayOnly, today.minusDays(1), 10);
        views(tomorrow, today.plusDays(1), 10);
        assertThat(issueIds()).containsExactly(yesterdayOnly);

        clock.set(today.plusDays(1).atTime(0, 1).atZone(SEOUL).toInstant());
        assertThat(issueIds()).containsExactly(tomorrow);
    }

    @Test
    void 로그인하지_않아도_볼_수_있다() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray());
    }
}
