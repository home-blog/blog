package com.myblog.stats.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestStats;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
 * 통계 읽기 (specs/006 US6, T039, quickstart S-8, S-7의 4, FR-031, FR-032, FR-036, FR-038, SC-007).
 * 숫자 줄은 통계 표에 바로 넣는다 (세기는 Phase 11). 지금 시각은 TestClock으로 정한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestStats.class, TestClock.Config.class})
class StatsQueryTest {

    /** 한국 시간 2026-10-08 오전 8시 = UTC 2026-10-07 23시. */
    private static final Instant KST_0800 = Instant.parse("2026-10-07T23:00:00Z");
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

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(KST_0800);
        owner = members.register("o");
        session = members.login(mvc, owner);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void S8_1_2_기간이_없으면_30일이고_7이면_7개_날짜_오늘이_마지막() throws Exception {
        mvc.perform(get("/api/manage/stats").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.daily", hasSize(30)))
                .andExpect(jsonPath("$.daily[0].date").value("2026-09-09"))
                .andExpect(jsonPath("$.daily[29].date").value("2026-10-08"));
        mvc.perform(get("/api/manage/stats").param("days", "7").session(session))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.daily", hasSize(7)))
                .andExpect(jsonPath("$.daily[0].date").value("2026-10-02"));
    }

    @Test
    void S8_4_기록이_없는_날도_빠지지_않고_0() throws Exception {
        stats.addBlogDay(owner.blogId(), TODAY.minusDays(2), 12, 8);
        stats.addBlogDay(owner.blogId(), TODAY.minusDays(40), 99, 99); // 기간 밖
        mvc.perform(get("/api/manage/stats").param("days", "7").session(session))
                .andExpect(jsonPath("$.daily[4].date").value("2026-10-06"))
                .andExpect(jsonPath("$.daily[4].views").value(12))
                .andExpect(jsonPath("$.daily[4].visitors").value(8))
                .andExpect(jsonPath("$.daily[5].views").value(0))
                .andExpect(jsonPath("$.daily[5].visitors").value(0))
                .andExpect(jsonPath("$.daily[5].comments").value(0));
    }

    @Test
    void S8_5_기간이_7_30이_아니면_400() throws Exception {
        for (String days : new String[] {"10", "0", "-7", "abc", "' OR 1=1 --"}) {
            mvc.perform(get("/api/manage/stats").param("days", days).session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mvc.perform(get("/api/manage/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void S7_4_한국_시간_오전_8시에_단_댓글은_오늘_날짜에_들어간다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        Member reader = members.register("r");
        addComment(postId, reader.id(), KST_0800);                             // 한국 10-08 08:00 → 오늘
        addComment(postId, reader.id(), Instant.parse("2026-10-07T14:59:59Z")); // 한국 10-07 23:59:59 → 어제
        addComment(postId, reader.id(), Instant.parse("2026-10-07T15:00:00Z")); // 한국 10-08 00:00 → 오늘
        mvc.perform(get("/api/manage/stats").param("days", "7").session(session))
                .andExpect(jsonPath("$.daily[6].date").value("2026-10-08"))
                .andExpect(jsonPath("$.daily[6].comments").value(2))
                .andExpect(jsonPath("$.daily[5].comments").value(1));
    }

    @Test
    void S8_6_응답에_유입_경로_시간대_기기_칸이_없다() throws Exception {
        String body = mvc.perform(get("/api/manage/stats").param("days", "7").session(session))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("referrer", "referer", "hour", "device", "userAgent");
    }

    @Test
    void 회원_B는_A의_숫자를_받지_않는다() throws Exception {
        stats.addBlogDay(owner.blogId(), TODAY, 50, 40);
        Member b = members.register("b");
        MockHttpSession bSession = members.login(mvc, b);
        mvc.perform(get("/api/manage/stats").param("days", "7").session(bSession))
                .andExpect(jsonPath("$.daily[6].views").value(0))
                .andExpect(jsonPath("$.daily[6].visitors").value(0));
    }

    private void addComment(Long postId, Long memberId, Instant createdAt) {
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '댓글', ?)",
                memberId, postId, Timestamp.from(createdAt));
    }
}
