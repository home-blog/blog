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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 글이 없는 목록은 오류가 아니다 (specs/004 US3, quickstart S-3, T021, FR-008). */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostListEmptyTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void S3_1_2_5_글이_없는_블로그는_방문자에게_0개이고_주인이면_isOwner() throws Exception {
        Member empty = members.register("c");
        mvc.perform(get("/api/blogs/{id}/posts", empty.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.isOwner").value(false));
        mvc.perform(get("/api/blogs/{id}/posts", empty.blogId()).session(members.login(mvc, empty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.isOwner").value(true));
    }

    @Test
    void S3_3_4_비공개_글만_있으면_방문자에게_0개이고_주인에게는_보인다() throws Exception {
        Member onlyPrivate = members.register("d");
        posts.add(onlyPrivate.defaultCategoryId(), "비공개", "본문", "private", Instant.parse("2026-01-01T00:00:00Z"));
        mvc.perform(get("/api/blogs/{id}/posts", onlyPrivate.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/blogs/{id}/posts", onlyPrivate.blogId()).session(members.login(mvc, onlyPrivate)))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.posts[0].visibility").value("private"));
    }
}
