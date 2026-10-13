package com.myblog.post.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
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
 * 블로그 머리의 "지금까지 쓴 원고지" (v0.5.1). 글 목록과 같은 공개 규칙으로 보이는 글만 세고,
 * 매수는 글마다 올림한 값(200자에 1매, 최소 1매)을 더한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class BlogManuscriptTotalTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;
    private Member owner;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        posts.add(owner.defaultCategoryId(), "긴 글", "가".repeat(250), "public", BASE);
        posts.add(owner.defaultCategoryId(), "짧은 글", "짧다", "public", BASE.plusSeconds(60));
        posts.add(owner.defaultCategoryId(), "비공개 글", "나만 본다", "private", BASE.plusSeconds(120));
    }

    @Test
    void 방문자에게는_공개_글만_세어_매수를_더한다() throws Exception {
        mvc.perform(get("/api/v1/blogs/{id}/manuscript", owner.blogId()).session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.postCount").value(2))
                .andExpect(jsonPath("$.sheets").value(3));
    }

    @Test
    void 주인에게는_비공개_글도_센다() throws Exception {
        MockHttpSession session = members.login(mvc, owner);
        mvc.perform(get("/api/v1/blogs/{id}/manuscript", owner.blogId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postCount").value(3))
                .andExpect(jsonPath("$.sheets").value(4));
    }

    @Test
    void 글이_없으면_0이다() throws Exception {
        Member empty = members.register("b");
        mvc.perform(get("/api/v1/blogs/{id}/manuscript", empty.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postCount").value(0))
                .andExpect(jsonPath("$.sheets").value(0));
    }

    @Test
    void 없는_블로그는_404다() throws Exception {
        mvc.perform(get("/api/v1/blogs/{id}/manuscript", 999_999))
                .andExpect(status().isNotFound());
    }
}
