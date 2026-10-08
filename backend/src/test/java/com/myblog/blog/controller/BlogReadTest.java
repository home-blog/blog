package com.myblog.blog.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 가입하면 내 블로그가 있고, 누구나 블로그와 분류 목록을 본다 (specs/003 US1, quickstart S-1, T013).
 * 가입 묶음(실패하면 모두 취소)은 SignupFlowTest에서 확인한다. 실제 PostgreSQL로 확인한다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class BlogReadTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

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
    }

    @Test
    void S1_2_내_블로그는_닉네임의_블로그이고_소개는_비어_있다() throws Exception {
        mvc.perform(get("/api/me/blog").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.name").value(owner.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.intro").value(""));
    }

    @Test
    void S1_3_분류는_미분류_하나이고_기본_분류다() throws Exception {
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("미분류"))
                .andExpect(jsonPath("$.categories[0].isDefault").value(true))
                .andExpect(jsonPath("$.categories[0].visibility").value("public"))
                .andExpect(jsonPath("$.categories[0].postCount").value(0));
    }

    @Test
    void S1_5_같은_회원의_블로그를_하나_더_만들_수_없다() {
        assertThatThrownBy(() -> jdbc.update("insert into blog (users_id, name) values (?, '둘째 블로그')", owner.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 로그인하지_않아도_블로그와_분류를_본다_주인이_아니다() throws Exception {
        mvc.perform(get("/api/blogs/{id}", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(owner.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.intro").value(""))
                .andExpect(jsonPath("$.isOwner").value(false));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("미분류"));
    }

    @Test
    void 주인이_보면_isOwner가_참이다() throws Exception {
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.isOwner").value(true));
    }

    @Test
    void 없는_블로그는_404() throws Exception {
        mvc.perform(get("/api/blogs/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
        mvc.perform(get("/api/blogs/999999999/categories"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
        mvc.perform(get("/api/blogs/abc")).andExpect(status().isNotFound());
    }

    @Test
    void 로그인하지_않으면_내_블로그는_401() throws Exception {
        mvc.perform(get("/api/me/blog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 분류는_주인이_정한_순서이고_방문자에게는_비공개_분류와_비공개_글이_빠진다() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 2);
        Long secret = members.addCategory(owner.blogId(), "비밀", "private", 0);
        members.addPost(owner.defaultCategoryId(), "공개", "public");
        members.addPost(owner.defaultCategoryId(), "비공개", "private");
        members.addPost(daily, "공개", "public");
        members.addPost(secret, "비밀 분류의 공개 글", "public");

        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.categories.length()").value(3))
                .andExpect(jsonPath("$.categories[0].categoryId").value(secret))
                .andExpect(jsonPath("$.categories[0].visibility").value("private"))
                .andExpect(jsonPath("$.categories[0].postCount").value(1))
                .andExpect(jsonPath("$.categories[1].postCount").value(2))
                .andExpect(jsonPath("$.categories[2].categoryId").value(daily));

        Member visitor = members.register("v");
        for (MockHttpSession session : new MockHttpSession[] {members.login(mvc, visitor), new MockHttpSession()}) {
            mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(session))
                    .andExpect(jsonPath("$.categories.length()").value(2))
                    .andExpect(jsonPath("$.categories[0].name").value("미분류"))
                    .andExpect(jsonPath("$.categories[0].postCount").value(1))
                    .andExpect(jsonPath("$.categories[1].categoryId").value(daily))
                    .andExpect(jsonPath("$.categories[1].postCount").value(1));
        }
    }

    @Test
    void 탈퇴한_회원의_세션은_방문자로_본다() throws Exception {
        jdbc.update("update users set deleted_at = now() where users_id = ?", owner.id());
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(false));
        mvc.perform(get("/api/me/blog").session(ownerSession)).andExpect(status().isUnauthorized());
    }
}
