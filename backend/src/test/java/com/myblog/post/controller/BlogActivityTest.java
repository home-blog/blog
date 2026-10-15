package com.myblog.post.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글쓰기 잔디 (specs/013 US1, S1-1 ~ S1-5). 오늘(한국 시간) 포함 365일, 공개 분류의 공개 글만 날짜별로 센다.
 * 지금은 한국 시간 2026-10-15 12:00 (세계 시간 03:00)으로 고정한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class, TestClock.Config.class})
class BlogActivityTest {

    /** 한국 시간 2026-10-15 12:00. */
    private static final Instant NOW = Instant.parse("2026-10-15T03:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private TestClock clock;

    private MockMvc mvc;
    private Member owner;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(NOW);
        owner = members.register("a");
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    @Test
    void S1_1_날짜별로_글_수와_매수를_센다() throws Exception {
        posts.add(owner.defaultCategoryId(), "오늘 1", "가".repeat(250), "public", NOW.minusSeconds(60));
        posts.add(owner.defaultCategoryId(), "오늘 2", "짧다", "public", NOW.minusSeconds(120));
        posts.add(owner.defaultCategoryId(), "어제", "짧다", "public", NOW.minusSeconds(86_400));

        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.from").value("2025-10-16"))
                .andExpect(jsonPath("$.to").value("2026-10-15"))
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-14"))
                .andExpect(jsonPath("$.days[0].postCount").value(1))
                .andExpect(jsonPath("$.days[0].sheets").value(1))
                .andExpect(jsonPath("$.days[1].date").value("2026-10-15"))
                .andExpect(jsonPath("$.days[1].postCount").value(2))
                .andExpect(jsonPath("$.days[1].sheets").value(3))
                .andExpect(jsonPath("$.totalPosts").value(3))
                .andExpect(jsonPath("$.totalSheets").value(4));
    }

    @Test
    void S1_2_비공개_글과_비공개_분류의_글은_주인이_봐도_세지_않는다() throws Exception {
        Long hidden = members.addCategory(owner.blogId(), "숨김", "private", 2);
        posts.add(owner.defaultCategoryId(), "공개", "짧다", "public", NOW.minusSeconds(60));
        posts.add(owner.defaultCategoryId(), "비공개", "짧다", "private", NOW.minusSeconds(60));
        posts.add(hidden, "숨긴 분류의 공개 글", "짧다", "public", NOW.minusSeconds(60));

        MockHttpSession session = members.login(mvc, owner);
        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(1))
                .andExpect(jsonPath("$.days[0].postCount").value(1))
                .andExpect(jsonPath("$.totalPosts").value(1));
    }

    @Test
    void S1_3_365일째_날은_세고_그보다_오래된_글은_세지_않는다() throws Exception {
        // 첫날 2025-10-16 한국 0시 = 2025-10-15T15:00Z
        posts.add(owner.defaultCategoryId(), "첫날 0시", "짧다", "public", Instant.parse("2025-10-15T15:00:00Z"));
        posts.add(owner.defaultCategoryId(), "그 1초 전", "짧다", "public", Instant.parse("2025-10-15T14:59:59Z"));

        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(1))
                .andExpect(jsonPath("$.days[0].date").value("2025-10-16"))
                .andExpect(jsonPath("$.totalPosts").value(1));
    }

    @Test
    void S1_4_날짜는_한국_시간으로_센다() throws Exception {
        // 한국 2026-10-15 00:10 = 세계 2026-10-14 15:10
        posts.add(owner.defaultCategoryId(), "새벽", "짧다", "public", Instant.parse("2026-10-14T15:10:00Z"));
        // 한국 2026-10-14 23:59 = 세계 2026-10-14 14:59
        posts.add(owner.defaultCategoryId(), "자정 전", "짧다", "public", Instant.parse("2026-10-14T14:59:00Z"));

        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId()))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-14"))
                .andExpect(jsonPath("$.days[0].postCount").value(1))
                .andExpect(jsonPath("$.days[1].date").value("2026-10-15"))
                .andExpect(jsonPath("$.days[1].postCount").value(1));
    }

    @Test
    void 글이_없으면_빈_칸이다() throws Exception {
        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(0))
                .andExpect(jsonPath("$.totalPosts").value(0))
                .andExpect(jsonPath("$.totalSheets").value(0));
    }

    @Test
    void S1_5_없는_블로그는_404다() throws Exception {
        mvc.perform(get("/api/v1/blogs/{id}/activity", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
    }
}
