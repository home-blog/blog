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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 수정 (specs/003 US4, quickstart S-7, S-2a의 5·6, T030). 바뀐 것이 없으면 DB도 그대로다 (SC-009).
 * 남의 글은 형식이 틀린 본문을 보내도 400이 아니라 404다 (contracts `요청 검사 순서`).
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class PostEditTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession ownerSession;
    private MockHttpSession otherSession;
    private Long postId;
    private Long travel;
    private Long dev;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        ownerSession = members.login(mvc, owner);
        otherSession = members.login(mvc, members.register("b"));
        postId = members.addPost(owner.defaultCategoryId(), "제목", "public", T0);
        travel = jdbc.queryForObject("select topic_id from topic where topic_name = '여행'", Long.class);
        dev = jdbc.queryForObject("select topic_id from topic where topic_name = '개발'", Long.class);
    }

    @Test
    void 수정_화면은_지금_값과_분류_주제_목록() throws Exception {
        mvc.perform(get("/api/posts/{id}/edit", postId).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.categoryId").value(owner.defaultCategoryId()))
                .andExpect(jsonPath("$.topicId").value(travel))
                .andExpect(jsonPath("$.visibility").value("public"))
                .andExpect(jsonPath("$.categories[0].name").value("미분류"))
                .andExpect(jsonPath("$.topics.length()").value(5));
    }

    @Test
    void S7_1_5_제목만_바꾸면_changed이고_수정_시각이_생기며_작성_시각은_그대로() throws Exception {
        update(postId, ownerSession, body("새 제목", "본문", owner.defaultCategoryId(), travel, "public", "createdAt", "2020-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        Map<String, Object> row = row();
        assertThat(row.get("title")).isEqualTo("새 제목");
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(T0);
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void S7_2_3_바꾸지_않거나_앞뒤_공백만_더하면_changed_false이고_DB도_그대로() throws Exception {
        update(postId, ownerSession, body("제목", "본문", owner.defaultCategoryId(), travel, "public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false))
                .andExpect(jsonPath("$.updatedAt").isEmpty());
        update(postId, ownerSession, body("  제목  ", "본문", owner.defaultCategoryId(), travel, null))
                .andExpect(jsonPath("$.changed").value(false));
        assertThat(row().get("updated_at")).isNull();
    }

    @Test
    void S7_4_공개_여부만_바꿔도_수정이다() throws Exception {
        update(postId, ownerSession, body("제목", "본문", owner.defaultCategoryId(), travel, "private"))
                .andExpect(jsonPath("$.changed").value(true));
        assertThat(row().get("visibility")).isEqualTo("private");
    }

    @Test
    void S2a_5_6_주제만_바꾸면_수정이고_주제를_빼면_400() throws Exception {
        update(postId, ownerSession, body("제목", "본문", owner.defaultCategoryId(), dev, "public"))
                .andExpect(jsonPath("$.changed").value(true));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.topic.name").value("개발"));

        update(postId, ownerSession, body("제목", "본문", owner.defaultCategoryId(), null, "public"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("topicId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("주제를 골라 주세요"));
        update(postId, ownerSession, body("제목", "본문", owner.defaultCategoryId(), 999999L, "public"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("주제를 골라 주세요"));
    }

    @Test
    void 분류를_내_다른_분류로_옮기고_남의_분류로는_못_옮긴다() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 2);
        update(postId, ownerSession, body("제목", "본문", daily, travel, "public")).andExpect(jsonPath("$.changed").value(true));
        assertThat(row().get("category_id")).isEqualTo(daily);

        Member other = members.register("c");
        update(postId, ownerSession, body("제목", "본문", other.defaultCategoryId(), travel, "public"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
        assertThat(row().get("category_id")).isEqualTo(daily);
    }

    @Test
    void 내_글의_입력_규칙은_글쓰기와_같다() throws Exception {
        update(postId, ownerSession, body("", "", owner.defaultCategoryId(), travel, "public"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("본문을 입력해 주세요"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("제목을 입력해 주세요"));
        update(postId, ownerSession, body("가".repeat(101), "본문", owner.defaultCategoryId(), travel, "public"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 제목은 100자 이하로 입력해 주세요"));
    }

    @Test
    void S7_6_7_남의_글은_수정_화면도_수정도_404이고_형식이_틀려도_404() throws Exception {
        mvc.perform(get("/api/posts/{id}/edit", postId).session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 글입니다"));
        update(postId, otherSession, body("남이 바꿈", "본문", owner.defaultCategoryId(), travel, "public"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        update(postId, otherSession, body("", "", null, null, "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        update(999999999L, ownerSession, body("", "", null, null, null)).andExpect(status().isNotFound());
        assertThat(row().get("title")).isEqualTo("제목");
    }

    @Test
    void 로그인하지_않으면_401이고_CSRF가_없으면_거절() throws Exception {
        mvc.perform(get("/api/posts/{id}/edit", postId)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/posts/{id}", postId).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/posts/{id}", postId).session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(body("새 제목", "본문", owner.defaultCategoryId(), travel, "public")))
                .andExpect(status().isForbidden());
    }

    private ResultActions update(Long id, MockHttpSession session, String json) throws Exception {
        return mvc.perform(put("/api/posts/{id}", id).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(String title, String content, Long categoryId, Long topicId, String visibility,
            Object... extra) {
        Object[] base = {"title", title, "content", content, "categoryId", categoryId, "topicId", topicId, "visibility", visibility};
        Object[] all = new Object[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return TestJson.of(all);
    }

    private Map<String, Object> row() {
        return jdbc.queryForMap("select * from post where post_id = ?", postId);
    }
}
