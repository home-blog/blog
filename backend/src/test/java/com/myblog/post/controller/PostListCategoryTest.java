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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * 분류로 좁혀 보기 (specs/004 US4, quickstart S-4, S-2a의 3·4, T023, T038).
 * 남의·없는·비공개 분류 번호와 숫자가 아닌 값은 모두 같은 404 "존재하지 않는 분류입니다"다 (D-2: 가, D-7).
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class PostListCategoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;
    private Member owner;
    private Long daily;
    private Long privateOnly;
    private Long secret;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        daily = members.addCategory(owner.blogId(), "일상", "public", 1);
        privateOnly = members.addCategory(owner.blogId(), "메모", "public", 2);
        secret = members.addCategory(owner.blogId(), "일기", "private", 3);
        for (int i = 1; i <= 13; i++) {
            posts.add(daily, "일상 " + i, "본문", "public", BASE.plus(i * 2L, ChronoUnit.HOURS));
            posts.add(owner.defaultCategoryId(), "미분류 " + i, "본문", "public", BASE.plus(i * 2L + 1, ChronoUnit.HOURS));
        }
        posts.add(privateOnly, "메모 비공개", "본문", "private", BASE);
        posts.add(secret, "숨김단어", "본문", "public", BASE);
    }

    @Test
    void S4_1_고르지_않으면_모든_분류의_공개_글() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()))
                .andExpect(jsonPath("$.totalCount").value(26))
                .andExpect(jsonPath("$.categoryId").doesNotExist());
    }

    @Test
    void S4_2_3_4_분류를_고르면_모든_페이지에_그_분류의_글만() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("categoryId", daily.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(daily))
                .andExpect(jsonPath("$.totalCount").value(13))
                .andExpect(jsonPath("$.totalPages").value(2));
        for (String page : new String[] {"1", "2"}) {
            String body = mvc.perform(get("/api/blogs/{id}/posts", owner.blogId())
                            .param("categoryId", daily.toString()).param("page", page))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            List<Number> categories = JsonPath.read(body, "$.posts[*].categoryId");
            assertThat(categories).hasSize(page.equals("1") ? 10 : 3)
                    .allSatisfy(id -> assertThat(id.longValue()).isEqualTo(daily));
            List<String> names = JsonPath.read(body, "$.posts[*].categoryName");
            assertThat(names).containsOnly("일상");
        }
    }

    @Test
    void S4_6_공개_글이_없는_분류는_방문자에게_0개() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("categoryId", privateOnly.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void S4_7_S2a_3_4_남의_없는_비공개_분류와_숫자가_아닌_값은_똑같이_404() throws Exception {
        Member other = members.register("b");
        posts.add(other.defaultCategoryId(), "B의 글", "본문", "public", BASE);
        String expected = mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("categoryId", "999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("※ 존재하지 않는 분류입니다"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (String categoryId : new String[] {other.defaultCategoryId().toString(), secret.toString(), "abc",
                "1 OR 1=1", "-1", "99999999999999999999"}) {
            String body = mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("categoryId", categoryId))
                    .andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(body).isEqualTo(expected);
        }
    }

    @Test
    void S2a_6_주인은_비공개_분류를_고를_수_있다() throws Exception {
        MockHttpSession ownerSession = members.login(mvc, owner);
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(ownerSession)
                        .param("categoryId", secret.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.posts[0].title").value("숨김단어"));
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).session(ownerSession)
                        .param("categoryId", privateOnly.toString()))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.posts[0].visibility").value("private"));
    }

    @Test
    void 빈_분류_값은_고르지_않은_것이다() throws Exception {
        mvc.perform(get("/api/blogs/{id}/posts", owner.blogId()).param("categoryId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(26));
    }
}
