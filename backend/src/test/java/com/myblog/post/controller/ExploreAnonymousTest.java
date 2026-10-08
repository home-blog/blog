package com.myblog.post.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 로그인하지 않아도 목록·분류별 목록·페이지 넘기기가 모두 된다 (specs/004 US7, quickstart S-7의 1, T035, SC-010). */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class ExploreAnonymousTest {

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
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 1; i <= 12; i++) {
            posts.add(owner.defaultCategoryId(), "글 " + i, "본문", "public", base.plus(i, ChronoUnit.HOURS));
        }
    }

    @Test
    void S7_1_로그인_없이_목록_분류별_목록_2페이지가_모두_200() throws Exception {
        for (MockHttpSession session : new MockHttpSession[] {null, expiredSession()}) {
            for (RequestBuilder request : new RequestBuilder[] {
                    withSession(get("/api/blogs/{id}/posts", owner.blogId()), session),
                    withSession(get("/api/blogs/{id}/posts", owner.blogId())
                            .param("categoryId", owner.defaultCategoryId().toString()), session),
                    withSession(get("/api/blogs/{id}/posts", owner.blogId()).param("page", "2"), session)}) {
                mvc.perform(request).andExpect(status().isOk());
            }
        }
    }

    /** 로그아웃한 뒤 남은 옛 세션 쿠키. */
    private MockHttpSession expiredSession() throws Exception {
        MockHttpSession session = members.login(mvc, owner);
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().is2xxSuccessful());
        return session;
    }

    private static RequestBuilder withSession(MockHttpServletRequestBuilder request, MockHttpSession session) {
        return session == null ? request : request.session(session);
    }
}
