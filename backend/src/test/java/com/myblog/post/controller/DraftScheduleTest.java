package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.post.service.DraftPublisher;
import com.myblog.support.TestClock;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * 예약 발행 (specs/013 US4, S4-1 ~ S4-3, S4-5, S4-6, contracts 4). 지금은 2026-10-15T03:00:00Z로 고정하고,
 * 1분마다 도는 작업 대신 DraftPublisher.publishDue()를 직접 부른다. 두 벌 동시 실행(S4-4)은 DraftScheduleConcurrencyTest.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestClock.Config.class})
class DraftScheduleTest {

    private static final Instant NOW = Instant.parse("2026-10-15T03:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DraftPublisher publisher;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;
    private Long topicId;
    private Long draftId;

    @BeforeEach
    void setUp() throws Exception {
        clock.set(NOW);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        session = members.login(mvc, owner);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        String body = mvc.perform(post("/api/v1/me/drafts").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("title", "쓰는 중", "content", ""))).andReturn().getResponse().getContentAsString();
        draftId = ((Number) JsonPath.read(body, "$.draftId")).longValue();
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    private String scheduleBody(String title, Long categoryId, Object publishAt) {
        return TestJson.of("title", title, "content", "예약 본문", "categoryId", categoryId, "topicId", topicId,
                "visibility", "public", "publishAt", publishAt == null ? null : publishAt.toString());
    }

    private ResultActions schedule(Object publishAt) throws Exception {
        return mvc.perform(put("/api/v1/me/drafts/{id}/schedule", draftId).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody("예약 글", owner.defaultCategoryId(), publishAt)));
    }

    private long visiblePostCount() throws Exception {
        String body = mvc.perform(get("/api/v1/blogs/{id}/posts", owner.blogId())).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.totalCount")).longValue();
    }

    @Nested
    @DisplayName("S4-1 시각 경계 (1분 뒤 ~ 30일)")
    class Bounds {

        @Test
        void 지금과_지난_시각은_거절한다() throws Exception {
            schedule(NOW).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("publishAt"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("PUBLISH_AT_TOO_SOON"));
            schedule(NOW.minusSeconds(3600)).andExpect(status().isBadRequest());
        }

        @Test
        void 일분_뒤와_30일_뒤는_된다() throws Exception {
            schedule(NOW.plusSeconds(60)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.publishAt").value("2026-10-15T03:01:00Z"));
            schedule(NOW.plus(Duration.ofDays(30))).andExpect(status().isOk());
        }

        @Test
        void 초는_버리고_센다() throws Exception {
            // 3:01:59 → 3:01:00 (1분 뒤라 된다), 3:00:59 → 3:00:00 (지금이라 안 된다)
            schedule(NOW.plusSeconds(119)).andExpect(status().isOk()).andExpect(jsonPath("$.publishAt").value("2026-10-15T03:01:00Z"));
            schedule(NOW.plusSeconds(59)).andExpect(status().isBadRequest());
        }

        @Test
        void 삼십일_1분_뒤는_거절한다() throws Exception {
            schedule(NOW.plus(Duration.ofDays(30)).plusSeconds(60)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("PUBLISH_AT_TOO_LATE"));
        }

        @Test
        void 시각이_없으면_거절한다() throws Exception {
            schedule(null).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("PUBLISH_AT_REQUIRED"));
        }

        @Test
        void 예약할_때도_보통_글쓰기_검사를_한다() throws Exception {
            mvc.perform(put("/api/v1/me/drafts/{id}/schedule", draftId).session(session).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(scheduleBody("", owner.defaultCategoryId(), NOW.plusSeconds(600))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
        }
    }

    @Test
    void S4_2_시각_전에는_보이지_않고_시각이_지나면_올라간다() throws Exception {
        schedule(NOW.plusSeconds(3600)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/drafts").session(session)).andExpect(jsonPath("$.items[0].publishAt").value("2026-10-15T04:00:00Z"));

        clock.set(NOW.plusSeconds(3599));
        assertThat(publisher.publishDue()).isZero();
        assertThat(visiblePostCount()).isZero();

        clock.set(NOW.plusSeconds(3630));
        assertThat(publisher.publishDue()).isOne();
        assertThat(visiblePostCount()).isOne();
        String list = mvc.perform(get("/api/v1/blogs/{id}/posts", owner.blogId())).andReturn().getResponse().getContentAsString();
        Long postId = ((Number) JsonPath.read(list, "$.posts[0].postId")).longValue();
        mvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(jsonPath("$.title").value("예약 글"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-15T04:00:30Z"));
        mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session)).andExpect(status().isNotFound());
        assertThat(publisher.publishDue()).as("한 번 올린 것은 다시 올리지 않는다").isZero();
    }

    @Test
    void S4_3_예약을_취소하면_시각이_지나도_올라가지_않는다() throws Exception {
        schedule(NOW.plusSeconds(600)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/me/drafts/{id}/schedule", draftId).session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishAt").value(nullValue()));
        clock.set(NOW.plusSeconds(3600));
        assertThat(publisher.publishDue()).isZero();
        assertThat(visiblePostCount()).isZero();
        mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session)).andExpect(jsonPath("$.title").value("예약 글"));
    }

    @Test
    void 예약된_글을_다시_저장해도_예약은_남는다() throws Exception {
        schedule(NOW.plusSeconds(600)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", "고친 예약 글", "content", "예약 본문", "categoryId", owner.defaultCategoryId(),
                                "topicId", topicId, "visibility", "public")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishAt").value("2026-10-15T03:10:00Z"));
    }

    @Test
    void S4_5_올리다_실패하면_예약을_풀고_임시_저장_글로_남긴다() throws Exception {
        schedule(NOW.plusSeconds(600)).andExpect(status().isOk());
        // 예약 뒤 저장으로 제목을 지웠다 (저장은 글자 수 상한만 본다)
        mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("title", "", "content", "예약 본문", "categoryId", owner.defaultCategoryId(), "topicId", topicId,
                        "visibility", "public"))).andExpect(status().isOk());
        clock.set(NOW.plusSeconds(3600));
        assertThat(publisher.publishDue()).isZero();
        assertThat(visiblePostCount()).isZero();
        mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishAt").value(nullValue()));
        assertThat(publisher.publishDue()).as("되풀이하지 않는다").isZero();
    }

    @Test
    void S4_6_예약된_글이_쓰는_분류는_지울_수_없다() throws Exception {
        String body = mvc.perform(post("/api/v1/me/blog/categories").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "예약 분류", "visibility", "public")))
                .andReturn().getResponse().getContentAsString();
        Long categoryId = ((Number) JsonPath.read(body, "$.categoryId")).longValue();
        mvc.perform(put("/api/v1/me/drafts/{id}/schedule", draftId).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody("예약 글", categoryId, NOW.plusSeconds(600))))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/me/blog/categories/{id}", categoryId).session(session).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_POSTS"));
        // 화면의 분류 글 수에는 넣지 않는다 (예약 글은 아직 목록에 없다)
        mvc.perform(get("/api/v1/blogs/{id}/categories", owner.blogId()).session(session))
                .andExpect(jsonPath("$.categories[?(@.categoryId == " + categoryId + ")].postCount").value(0));

        mvc.perform(delete("/api/v1/me/drafts/{id}/schedule", draftId).session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/me/blog/categories/{id}", categoryId).session(session).with(csrf()))
                .andExpect(status().isNoContent());
        // 테스트 트랜잭션은 요청끼리 같은 영속성 문맥을 써서 화면 응답은 지우기 전 값을 볼 수 있다. DB에서 직접 본다
        assertThat(jdbc.queryForObject("select category_id from post_draft where post_draft_id = ?", Long.class, draftId)).isNull();
    }

    @Test
    void 남의_임시_저장_글은_예약할_수_없다() throws Exception {
        MockHttpSession other = members.login(mvc, members.register("b"));
        mvc.perform(put("/api/v1/me/drafts/{id}/schedule", draftId).session(other).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody("뺏기", owner.defaultCategoryId(), NOW.plusSeconds(600))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/me/drafts/{id}/schedule", draftId).session(other).with(csrf())).andExpect(status().isNotFound());
    }
}
