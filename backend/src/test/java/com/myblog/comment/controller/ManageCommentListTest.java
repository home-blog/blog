package com.myblog.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 * 블로그 관리의 댓글 관리 (specs/006 US4, T026, quickstart S-4, FR-023 ~ FR-026, SC-003).
 * 댓글은 표에 바로 넣는다 (5초 간격과 상관없이 작성 시각을 정하려고). 삭제는 005의 주소로 확인한다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class ManageCommentListTest {

    private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private Member reader;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("o");
        reader = members.register("r");
        session = members.login(mvc, owner);
    }

    @Test
    void S4_1_댓글이_없으면_빈_목록() throws Exception {
        members.addPost(owner.defaultCategoryId(), "글", "public");
        mvc.perform(get("/api/manage/comments").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void S4_2_여러_글의_댓글_11개는_새_댓글이_위이고_10개_1개() throws Exception {
        Long first = members.addPost(owner.defaultCategoryId(), "첫 글", "public");
        Long second = members.addPost(owner.defaultCategoryId(), "둘째 글", "private");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            ids.add(addComment(i % 2 == 0 ? first : second, reader.id(), "댓글" + i, BASE.plusSeconds(i * 60L)));
        }
        String page1 = mvc.perform(get("/api/manage/comments").session(session))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.totalCount").value(11))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andReturn().getResponse().getContentAsString();
        String page2 = mvc.perform(get("/api/manage/comments").param("page", "2").session(session))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        List<Long> seen = new ArrayList<>(commentIds(page1));
        seen.addAll(commentIds(page2));
        assertThat(seen).containsExactlyElementsOf(ids.reversed());
    }

    @Test
    void S4_3_닉네임_작성시각_앞_50자_글_번호와_제목이_있고_이모지도_한_글자() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "여행 글", "public");
        String body = "😀".repeat(10) + "가".repeat(50);
        addComment(postId, reader.id(), body, BASE);
        mvc.perform(get("/api/manage/comments").session(session))
                .andExpect(jsonPath("$.items[0].author.nickname").value(reader.user().getNickname()))
                .andExpect(jsonPath("$.items[0].author.withdrawn").value(false))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].preview").value("😀".repeat(10) + "가".repeat(40)))
                .andExpect(jsonPath("$.items[0].post.postId").value(postId))
                .andExpect(jsonPath("$.items[0].post.title").value("여행 글"))
                .andExpect(jsonPath("$.items[0].isNew").value(false));
    }

    @Test
    void S4_5_탈퇴한_작성자는_닉네임_없이_withdrawn() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        addComment(postId, reader.id(), "탈퇴 전에 쓴 댓글", BASE);
        jdbc.update("update users set deleted_at = now(), nickname = '탈퇴한사용자' || users_id where users_id = ?", reader.id());
        mvc.perform(get("/api/manage/comments").session(session))
                .andExpect(jsonPath("$.items[0].author.withdrawn").value(true))
                .andExpect(jsonPath("$.items[0].author.nickname").doesNotExist());
    }

    @Test
    void S4_6_주인은_남의_댓글을_005의_주소로_지운다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        Long commentId = addComment(postId, reader.id(), "지울 댓글", BASE);
        mvc.perform(delete("/api/comments/{id}", commentId).with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/manage/comments").session(session)).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void S4_7_스크립트가_든_댓글은_글자_그대로() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        addComment(postId, reader.id(), "<script>alert(1)</script>", BASE);
        mvc.perform(get("/api/manage/comments").session(session))
                .andExpect(jsonPath("$.items[0].preview").value("<script>alert(1)</script>"));
    }

    @Test
    void 회원_B는_A의_블로그_댓글을_받지_않는다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "A의 글", "public");
        addComment(postId, reader.id(), "A 글의 댓글", BASE);
        MockHttpSession readerSession = members.login(mvc, reader);
        mvc.perform(get("/api/manage/comments").session(readerSession))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void 쪽_번호와_newSince가_틀리면_400이고_로그인하지_않으면_401() throws Exception {
        for (String[] query : List.of(new String[] {"page", "0"}, new String[] {"page", "-1"}, new String[] {"page", "abc"},
                new String[] {"newSince", "어제"}, new String[] {"newSince", "' OR 1=1 --"})) {
            mvc.perform(get("/api/manage/comments").param(query[0], query[1]).session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mvc.perform(get("/api/manage/comments")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/manage/comments/read").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/manage/comments/new-count")).andExpect(status().isUnauthorized());
    }

    private Long addComment(Long postId, Long memberId, String body, Instant createdAt) {
        return jdbc.queryForObject("insert into comment (users_id, post_id, body, created_at) values (?, ?, ?, ?)"
                + " returning comment_id", Long.class, memberId, postId, body, Timestamp.from(createdAt));
    }

    private static List<Long> commentIds(String body) {
        List<Number> ids = JsonPath.read(body, "$.items[*].commentId");
        return ids.stream().map(Number::longValue).toList();
    }
}
