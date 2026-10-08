package com.myblog.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * 새 댓글 표시 (specs/006 US5, T030, quickstart S-5의 1 ~ 6, FR-027 ~ FR-029, SC-008).
 * 메뉴 옆(GET /api/manage/blog)과 new-count가 같은 숫자다. 대시보드 숫자는 DashboardTest에서 본다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class NewCommentCountTest {

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
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("o");
        reader = members.register("r");
        session = members.login(mvc, owner);
        postId = members.addPost(owner.defaultCategoryId(), "글", "public");
    }

    @Test
    void S5_1_한_번도_열지_않았으면_남이_단_모든_댓글이고_주인_댓글은_세지_않는다() throws Exception {
        Instant past = Instant.now().minus(3, ChronoUnit.DAYS);
        addComment(reader.id(), past);
        addComment(reader.id(), past.plusSeconds(10));
        addComment(owner.id(), past.plusSeconds(20));
        expectCount(2);
    }

    @Test
    void S5_2_3_4_읽음_처리하면_0이고_그_뒤에_달린_하나만_1() throws Exception {
        addComment(reader.id(), Instant.now().minus(1, ChronoUnit.HOURS));
        String read = mvc.perform(post("/api/manage/comments/read").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousReadAt").doesNotExist())
                .andExpect(jsonPath("$.readAt").exists())
                .andReturn().getResponse().getContentAsString();
        expectCount(0);

        addComment(reader.id(), Instant.now().plus(1, ChronoUnit.MINUTES));
        addComment(owner.id(), Instant.now().plus(2, ChronoUnit.MINUTES));
        expectCount(1);

        String again = mvc.perform(post("/api/manage/comments/read").with(csrf()).session(session))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(again, "$.previousReadAt")).isEqualTo(JsonPath.read(read, "$.readAt"));
    }

    @Test
    void S5_5_6_newSince로_쪽을_넘겨도_NEW는_같은_댓글에만() throws Exception {
        Instant since = Instant.parse("2026-09-10T00:00:00Z");
        for (int i = 0; i < 12; i++) {
            addComment(reader.id(), since.minusSeconds(540).plusSeconds(i * 60L)); // 0~9: 같거나 이전, 10·11: 이후
        }
        addComment(owner.id(), since.plusSeconds(3600)); // 주인이 쓴 것은 NEW가 아니다
        String newSince = "2026-09-10T09:00:00+09:00";
        String page1 = mvc.perform(get("/api/manage/comments").param("newSince", newSince).session(session))
                .andReturn().getResponse().getContentAsString();
        String page2 = mvc.perform(get("/api/manage/comments").param("newSince", newSince).param("page", "2").session(session))
                .andReturn().getResponse().getContentAsString();
        List<Boolean> flags = new java.util.ArrayList<>(JsonPath.<List<Boolean>>read(page1, "$.items[*].isNew"));
        flags.addAll(JsonPath.<List<Boolean>>read(page2, "$.items[*].isNew"));
        // 최신순: 주인 댓글(아님), 11·10(NEW), 나머지(아님)
        assertThat(flags).containsExactly(false, true, true, false, false, false, false, false, false, false, false, false,
                false);
        // newSince가 없으면 NEW 없음, never면 남이 쓴 모든 댓글
        mvc.perform(get("/api/manage/comments").session(session)).andExpect(jsonPath("$.items[1].isNew").value(false));
        mvc.perform(get("/api/manage/comments").param("newSince", "never").session(session))
                .andExpect(jsonPath("$.items[0].isNew").value(false))
                .andExpect(jsonPath("$.items[1].isNew").value(true))
                .andExpect(jsonPath("$.items[9].isNew").value(true));
    }

    @Test
    void CSRF_토큰_없이_읽음_처리하면_거절되고_숫자는_그대로() throws Exception {
        addComment(reader.id(), Instant.now().minus(1, ChronoUnit.HOURS));
        mvc.perform(post("/api/manage/comments/read").session(session)).andExpect(status().isForbidden());
        expectCount(1);
    }

    @Test
    void 남의_블로그_댓글은_내_숫자에_들지_않는다() throws Exception {
        Long othersPost = members.addPost(reader.defaultCategoryId(), "남의 글", "public");
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '댓글', now())", owner.id(), othersPost);
        expectCount(0);
    }

    private void expectCount(long count) throws Exception {
        mvc.perform(get("/api/manage/comments/new-count").session(session)).andExpect(jsonPath("$.count").value(count));
        mvc.perform(get("/api/manage/blog").session(session)).andExpect(jsonPath("$.newCommentCount").value(count));
    }

    private void addComment(Long memberId, Instant createdAt) {
        jdbc.update("insert into comment (users_id, post_id, body, created_at) values (?, ?, '댓글', ?)",
                memberId, postId, Timestamp.from(createdAt));
    }
}
