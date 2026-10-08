package com.myblog.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.web.context.WebApplicationContext;

/**
 * 댓글 삭제 (specs/005 US2, T022, quickstart S-4의 3 ~ 9, FR-004, FR-005, FR-029, SC-006).
 * 남의 댓글은 없는 댓글과 같은 404 COMMENT_NOT_FOUND (2026-10-08 결정).
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentDeleteTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private Member writer;
    private MockHttpSession ownerSession;
    private MockHttpSession writerSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        writer = register("w");
        ownerSession = members.login(mvc, owner);
        writerSession = members.login(mvc, writer);
        postId = members.addPost(owner.defaultCategoryId(), "댓글 달 글", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S4_3_작성자는_자기_댓글을_지우고_줄이_실제로_없어진다() throws Exception {
        Long commentId = write(writerSession, postId);
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        assertThat(exists(commentId)).isFalse();
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(writerSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void S4_5_블로그_주인은_자기_글의_남의_댓글도_지운다() throws Exception {
        Long commentId = write(writerSession, postId);
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(ownerSession))
                .andExpect(status().isNoContent());
        assertThat(exists(commentId)).isFalse();
    }

    @Test
    void S4_6_7_다른_회원과_남의_블로그_주인은_없는_댓글과_같은_404이고_그대로() throws Exception {
        Long commentId = write(writerSession, postId);
        Member stranger = register("s");
        MockHttpSession strangerSession = members.login(mvc, stranger);
        // 남의 블로그 주인: 다른 블로그의 주인도 이 글의 댓글은 못 지운다
        members.addPost(stranger.defaultCategoryId(), "남의 글", "public");

        String missing = mvc.perform(delete("/api/comments/{id}", 99_999_999L).with(csrf()).session(strangerSession))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String others = mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(strangerSession))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(others).isEqualTo(missing).contains("COMMENT_NOT_FOUND");
        assertThat(exists(commentId)).isTrue();
    }

    @Test
    void S4_8_로그인하지_않으면_401이고_CSRF가_없으면_거절() throws Exception {
        Long commentId = write(writerSession, postId);
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/comments/{id}", commentId).session(writerSession)).andExpect(status().isForbidden());
        assertThat(exists(commentId)).isTrue();
    }

    @Test
    void S4_9_볼_수_없게_된_글의_댓글은_작성자도_404() throws Exception {
        Long commentId = write(writerSession, postId);
        jdbc.update("update post set visibility = 'private' where post_id = ?", postId);
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(writerSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        assertThat(exists(commentId)).isTrue();
        // 주인은 자기 비공개 글의 댓글을 지운다
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(ownerSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void S4_FR005_댓글을_고치는_주소는_없다() throws Exception {
        Long commentId = write(writerSession, postId);
        mvc.perform(put("/api/comments/{id}", commentId).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "고침")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/comments/{id}", commentId).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "고침")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(jdbc.queryForObject("select body from comment where comment_id = ?", String.class, commentId))
                .isEqualTo("댓글");
    }

    private Long write(MockHttpSession session, Long target) throws Exception {
        String response = mvc.perform(post("/api/posts/{id}/comments", target).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "댓글")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private boolean exists(Long commentId) {
        return jdbc.queryForObject("select count(*) from comment where comment_id = ?", Long.class, commentId) > 0;
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
