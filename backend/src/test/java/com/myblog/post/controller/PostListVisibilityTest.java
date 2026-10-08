package com.myblog.post.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
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
 * 공개 글은 누구나, 비공개 글과 비공개 분류의 글은 주인만 (specs/004 US2, quickstart S-2, S-2a의 1·2·6, T018).
 * 화면이 보낸 값으로 이 규칙을 바꿀 수 없고, 만료·탈퇴한 세션은 401이 아니라 방문자다 (SC-010).
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostListVisibilityTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession ownerSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        ownerSession = members.login(mvc, owner);
        for (int i = 1; i <= 29; i++) {
            posts.add(owner.defaultCategoryId(), "공개 " + i, "본문", "public", BASE.plus(i, ChronoUnit.HOURS));
        }
        posts.add(owner.defaultCategoryId(), "비공개 1", "본문", "private", BASE.plus(100, ChronoUnit.HOURS));
        posts.add(owner.defaultCategoryId(), "비공개 2", "본문", "private", BASE.plus(101, ChronoUnit.HOURS));
    }

    @Test
    void S2_1_2_방문자와_다른_회원에게는_비공개_글이_없다() throws Exception {
        Member other = members.register("b");
        for (MockHttpSession session : new MockHttpSession[] {new MockHttpSession(), members.login(mvc, other)}) {
            mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isOwner").value(false))
                    .andExpect(jsonPath("$.totalCount").value(29))
                    .andExpect(jsonPath("$.posts[0].title").value("공개 29"))
                    .andExpect(jsonPath("$.posts[*].visibility", Matchers.everyItem(Matchers.is("public"))));
        }
    }

    @Test
    void S2_3_주인에게는_비공개_글도_보이고_표시가_있다() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.totalCount").value(31))
                .andExpect(jsonPath("$.posts[0].title").value("비공개 2"))
                .andExpect(jsonPath("$.posts[0].visibility").value("private"))
                .andExpect(jsonPath("$.posts[2].visibility").value("public"));
    }

    @Test
    void S2_4_요청_값으로_비공개를_달라고_해도_무시한다() throws Exception {
        MockHttpSession otherSession = members.login(mvc, members.register("b"));
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(otherSession)
                        .param("visibility", "private").param("includePrivate", "true").param("isOwner", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.totalCount").value(29));
    }

    @Test
    void S2_6_로그아웃한_뒤의_옛_쿠키는_401이_아니라_방문자다() throws Exception {
        MockHttpSession session = members.login(mvc, owner);
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.totalCount").value(29));
    }

    @Test
    void 탈퇴한_회원의_세션은_방문자로_본다() throws Exception {
        jdbc.update("update users set deleted_at = now() where users_id = ?", owner.id());
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.totalCount").value(29));
    }

    @Test
    void S2a_1_6_비공개_분류의_공개_글은_방문자에게_없고_주인에게는_있다() throws Exception {
        Member diary = members.register("d");
        Long secret = members.addCategory(diary.blogId(), "일기", "private", 1);
        posts.add(secret, "숨김단어 1", "본문", "public", BASE);
        posts.add(secret, "숨김단어 2", "본문", "public", BASE.plusSeconds(1));
        posts.add(diary.defaultCategoryId(), "비공개 글", "본문", "private", BASE);

        for (MockHttpSession session : new MockHttpSession[] {new MockHttpSession(),
                members.login(mvc, members.register("b"))}) {
            mvc.perform(get("/api/blogs/{id}/posts", diary.blogId()).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.posts.length()").value(0));
            mvc.perform(get("/api/blogs/{id}/categories", diary.blogId()).session(session))
                    .andExpect(jsonPath("$.categories[*].name", Matchers.not(Matchers.hasItem("일기"))));
        }

        mvc.perform(get("/api/blogs/{id}/posts", diary.blogId()).session(members.login(mvc, diary)))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.posts[0].title").value("숨김단어 2"))
                .andExpect(jsonPath("$.posts[0].categoryName").value("일기"));
    }
}
