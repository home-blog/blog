package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글·분류의 공개 범위 (specs/003 US5, quickstart S-9, S-9a의 글 쪽, T038).
 * 바꾸면 그 요청이 끝나는 순간부터 반영되고(캐시 없음), 이전·다음 글과 분류 개수에도 비공개가 섞이지 않는다.
 * 분류의 공개 여부를 바꾸는 주소는 US6이라, 여기서는 표를 바로 고쳐 준비한다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class PostVisibilityTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession ownerSession;
    private MockHttpSession otherSession;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        ownerSession = members.login(mvc, owner);
        otherSession = members.login(mvc, members.register("b"));
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    @Test
    void S9_1_2_10_비공개_글은_주인만_읽고_공개에서_비공개로_바꾸면_바로_404() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public", T0);
        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(status().isOk());

        setVisibility(postId, owner.defaultCategoryId(), "private");
        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}", postId).session(ownerSession)).andExpect(status().isOk());

        setVisibility(postId, owner.defaultCategoryId(), "public");
        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(status().isOk());
    }

    @Test
    void S9a_1_6_7_분류를_비공개로_해도_글의_visibility는_그대로이고_미분류도_비공개가_되며_다시_공개하면_보인다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "미분류의 글", "public", T0);
        setCategoryVisibility(owner.defaultCategoryId(), "private");

        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select visibility from post where post_id = ?", String.class, postId)).isEqualTo("public");
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(otherSession))
                .andExpect(jsonPath("$.categories.length()").value(0));

        setCategoryVisibility(owner.defaultCategoryId(), "public");
        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(status().isOk());
    }

    @Test
    void S9a_8_비공개_분류의_공개_글을_공개_분류로_옮기면_그_글만_보인다() throws Exception {
        Long diary = members.addCategory(owner.blogId(), "일기", "private", 2);
        Long moved = members.addPost(diary, "옮길 글", "public", T0);
        Long stays = members.addPost(diary, "남을 글", "public", T0.plus(1, ChronoUnit.HOURS));

        setVisibility(moved, owner.defaultCategoryId(), "public");
        mvc.perform(get("/api/posts/{id}", moved).session(otherSession)).andExpect(status().isOk());
        mvc.perform(get("/api/posts/{id}", stays).session(otherSession)).andExpect(status().isNotFound());
    }

    @Test
    void 이전_다음과_분류_개수에_비공개가_섞이지_않는다() throws Exception {
        Long diary = members.addCategory(owner.blogId(), "일기", "private", 2);
        Long first = members.addPost(owner.defaultCategoryId(), "1", "public", T0);
        members.addPost(owner.defaultCategoryId(), "비공개 글", "private", T0.plus(1, ChronoUnit.HOURS));
        members.addPost(diary, "비공개 분류의 글", "public", T0.plus(2, ChronoUnit.HOURS));
        Long last = members.addPost(owner.defaultCategoryId(), "2", "public", T0.plus(3, ChronoUnit.HOURS));

        mvc.perform(get("/api/posts/{id}", first).session(otherSession)).andExpect(jsonPath("$.nextPostId").value(last));
        mvc.perform(get("/api/posts/{id}", last).session(ownerSession)).andExpect(jsonPath("$.prevPostId").value(first));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(otherSession))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].postCount").value(2));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.categories[0].postCount").value(3))
                .andExpect(jsonPath("$.categories[1].postCount").value(1));
    }

    /** 표를 바로 고친다. 테스트 전체가 한 트랜잭션이라 JPA가 기억한 분류를 비워야 새 값을 읽는다. */
    private void setCategoryVisibility(Long categoryId, String visibility) {
        entityManager.flush();
        jdbc.update("update category set visibility = ? where category_id = ?", visibility, categoryId);
        entityManager.clear();
    }

    private void setVisibility(Long postId, Long categoryId, String visibility) throws Exception {
        mvc.perform(put("/api/posts/{id}", postId).with(csrf()).session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", jdbc.queryForObject("select title from post where post_id = ?", String.class, postId),
                                "content", "본문", "categoryId", categoryId, "topicId", topicId, "visibility", visibility)))
                .andExpect(status().isOk());
    }
}
