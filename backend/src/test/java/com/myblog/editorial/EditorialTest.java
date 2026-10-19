package com.myblog.editorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 편집실 (specs/018 US1 ~ US3, S1-1 ~ S3-2). 우편함 소식은 커밋 뒤 따로 만들어지므로 클래스 전체 @Transactional을 쓰지 않고
 * 끝에서 지운다. 다른 테스트의 신고와 섞이지 않게 목록 대신 글 하나의 자세히·처리 결과로 확인한다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
class EditorialTest {

    private static final long WAIT_MS = 5000;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member writer;
    private Member reporter;
    private Member admin;
    private MockHttpSession writerSession;
    private MockHttpSession reporterSession;
    private MockHttpSession adminSession;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(LocalDate.of(2100, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1, 300_000)).atTime(3, 0)
                .toInstant(ZoneOffset.UTC));
        writer = register("ew");
        reporter = register("er");
        admin = register("ea");
        jdbc.update("update users set role = 'admin' where users_id = ?", admin.id());
        writerSession = members.login(mvc, writer);
        reporterSession = members.login(mvc, reporter);
        adminSession = members.login(mvc, admin);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        for (Member member : created) {
            jdbc.update("delete from notification where users_id = ?", member.id());
            jdbc.update("delete from editorial_action where users_id = ?", member.id());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private Long publicPost(String title) {
        return members.addPost(writer.defaultCategoryId(), title, "public");
    }

    private void report(MockHttpSession session, Long postId, String reason) throws Exception {
        mvc.perform(post("/api/v1/posts/{id}/reports", postId).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", reason, "detail", "광고 링크")))
                .andExpect(status().isCreated());
    }

    private ResultActions takeDown(MockHttpSession session, Long postId, String reason) throws Exception {
        return mvc.perform(post("/api/v1/editorial/posts/{id}/take-down", postId).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", reason)));
    }

    private long notices(Member member, String kind) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        long count = 0;
        while (System.currentTimeMillis() < deadline) {
            count = jdbc.queryForObject("select count(*) from notification where users_id = ? and kind = ?", Long.class,
                    member.id(), kind);
            if (count > 0) {
                return count;
            }
            Thread.sleep(50);
        }
        return count;
    }

    @Test
    void S1_1_관리자가_아니면_403이고_로그인하지_않으면_401() throws Exception {
        mvc.perform(get("/api/v1/editorial/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/editorial/summary").session(writerSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/editorial/reports/posts").session(writerSession)).andExpect(status().isForbidden());
        takeDown(writerSession, publicPost("남의 글"), "사유").andExpect(status().isForbidden());
        // 본문이 없어도 관리자 확인이 먼저다
        mvc.perform(post("/api/v1/editorial/posts/{id}/take-down", publicPost("본문 없음")).with(csrf())
                .session(writerSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/editorial/summary").session(adminSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.members").isNumber())
                .andExpect(jsonPath("$.pendingReports").isNumber());
    }

    @Test
    void S1_신고가_검토_대기에_오르고_내리면_비공개로_잠기며_주인에게_소식이_간다() throws Exception {
        Long postId = publicPost("스팸 글");
        report(reporterSession, postId, "SPAM");
        Member second = register("er2");
        report(members.login(mvc, second), postId, "OTHER");

        mvc.perform(get("/api/v1/editorial/reports/posts/{id}", postId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("스팸 글"))
                .andExpect(jsonPath("$.reports", hasSize(2)))
                .andExpect(jsonPath("$.reports[0].reason").value("SPAM"))
                .andExpect(jsonPath("$.reports[0].detail").value(nullValue()))
                .andExpect(jsonPath("$.reports[1].detail").value("광고 링크"));
        String pending = mvc.perform(get("/api/v1/editorial/reports/posts?page=1").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> ids = JsonPath.read(pending, "$.items[*].postId");
        assertThat(ids.stream().map(Integer::longValue).toList()).contains(postId);

        // 사유는 1~200자
        takeDown(adminSession, postId, "  ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EDITORIAL_REASON_INVALID"));
        takeDown(adminSession, postId, "가".repeat(201)).andExpect(status().isBadRequest());
        takeDown(adminSession, postId, "광고 글이라 내립니다").andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select visibility from post where post_id = ?", String.class, postId))
                .isEqualTo("private");
        assertThat(jdbc.queryForObject("select count(*) from post_report where post_id = ? and resolution = 'taken_down'",
                Long.class, postId)).isEqualTo(2);
        mvc.perform(get("/api/v1/posts/{id}", postId)).andExpect(status().isNotFound());
        // S1-2 다시 처리하면 409
        takeDown(adminSession, postId, "또").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_RESOLVED"));

        assertThat(notices(writer, "editorial")).isEqualTo(1);
        mvc.perform(get("/api/v1/me/notifications").session(writerSession))
                .andExpect(jsonPath("$.items[0].kind").value("editorial"))
                .andExpect(jsonPath("$.items[0].postTitle").value("스팸 글"))
                .andExpect(jsonPath("$.items[0].note").value("광고 글이라 내립니다"))
                .andExpect(jsonPath("$.items[0].actorName").value("편집실"))
                .andExpect(jsonPath("$.items[0].available").value(true));
        mvc.perform(get("/api/v1/editorial/actions").session(adminSession))
                .andExpect(jsonPath("$.items[0].action").value("taken_down"))
                .andExpect(jsonPath("$.items[0].reportCount").value(2));
    }

    @Test
    void S1_3_내린_글은_주인이_공개로_바꿀_수_없고_잠금을_풀면_된다() throws Exception {
        Long postId = publicPost("잠길 글");
        report(reporterSession, postId, "ABUSE");
        takeDown(adminSession, postId, "욕설").andExpect(status().isNoContent());

        String toPublic = TestJson.of("title", "고친 글", "content", "본문", "categoryId", writer.defaultCategoryId(),
                "topicId", topicId, "visibility", "public");
        String toPrivate = TestJson.of("title", "고친 글", "content", "본문", "categoryId", writer.defaultCategoryId(),
                "topicId", topicId, "visibility", "private");
        mvc.perform(get("/api/v1/posts/{id}/edit", postId).session(writerSession))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockReason").value("욕설"));
        mvc.perform(put("/api/v1/posts/{id}", postId).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(toPublic))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("POST_LOCKED_BY_EDITOR"));
        mvc.perform(put("/api/v1/posts/{id}", postId).with(csrf()).session(writerSession)
                .contentType(MediaType.APPLICATION_JSON).content(toPrivate)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/editorial/posts/{id}/unlock", postId).with(csrf()).session(adminSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unlocked").value(true));
        // 이미 풀린 글은 그대로이고 기록도 남기지 않는다
        mvc.perform(post("/api/v1/editorial/posts/{id}/unlock", postId).with(csrf()).session(adminSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unlocked").value(false));
        assertThat(jdbc.queryForObject("select count(*) from editorial_action where post_id = ? and action = 'unlocked'",
                Long.class, postId)).isEqualTo(1);
        mvc.perform(put("/api/v1/posts/{id}", postId).with(csrf()).session(writerSession)
                .contentType(MediaType.APPLICATION_JSON).content(toPublic)).andExpect(status().isOk());
    }

    @Test
    void S1_5_반려하면_글은_그대로이고_새_신고가_오면_다시_대기에_오른다() throws Exception {
        Long postId = publicPost("멀쩡한 글");
        report(reporterSession, postId, "ADULT");
        mvc.perform(post("/api/v1/editorial/posts/{id}/dismiss", postId).with(csrf()).session(adminSession))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/editorial/posts/{id}/dismiss", postId).with(csrf()).session(adminSession))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select visibility from post where post_id = ?", String.class, postId))
                .isEqualTo("public");
        mvc.perform(get("/api/v1/editorial/reports/posts/{id}", postId).session(adminSession))
                .andExpect(jsonPath("$.reports", hasSize(0)));

        Member second = register("er3");
        report(members.login(mvc, second), postId, "SPAM");
        mvc.perform(get("/api/v1/editorial/reports/posts/{id}", postId).session(adminSession))
                .andExpect(jsonPath("$.reports", hasSize(1)));
    }

    @Test
    void S2_글마당_신고와_지우기() throws Exception {
        String body = mvc.perform(post("/api/v1/board/posts").with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("topic", "여행", "kind", "talk", "title", "광고 글마당", "content", "사세요")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Long boardPostId = ((Number) JsonPath.read(body, "$.boardPostId")).longValue();
        String url = "/api/v1/board/posts/{id}/reports";

        // S2-1 자기 글 403, 없는 사유 400, 두 번 409, 없는 글 404
        mvc.perform(post(url, boardPostId).with(csrf()).session(writerSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("reason", "SPAM"))).andExpect(status().isForbidden());
        mvc.perform(post(url, boardPostId).with(csrf()).session(reporterSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("reason", "MEAN"))).andExpect(status().isBadRequest());
        mvc.perform(post(url, boardPostId).with(csrf()).session(reporterSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("reason", "SPAM"))).andExpect(status().isCreated());
        mvc.perform(post(url, boardPostId).with(csrf()).session(reporterSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("reason", "SPAM"))).andExpect(status().isConflict());
        mvc.perform(post(url, Long.MAX_VALUE).with(csrf()).session(reporterSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("reason", "SPAM"))).andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/editorial/reports/board/{id}", boardPostId).session(adminSession))
                .andExpect(jsonPath("$.title").value("광고 글마당"))
                .andExpect(jsonPath("$.reports", hasSize(1)));
        // 반려한 뒤에는 지울 대기 신고가 없어 409, 새 신고가 오면 다시 지울 수 있다
        mvc.perform(post("/api/v1/editorial/board/{id}/dismiss", boardPostId).with(csrf()).session(adminSession))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/editorial/board/{id}/remove", boardPostId).with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "광고")))
                .andExpect(status().isConflict());
        Member second = register("er4");
        mvc.perform(post(url, boardPostId).with(csrf()).session(members.login(mvc, second))
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "SPAM"))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/editorial/board/{id}/remove", boardPostId).with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "광고")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/board/posts/{id}", boardPostId)).andExpect(status().isNotFound());
        assertThat(notices(writer, "editorial")).isEqualTo(1);
        mvc.perform(get("/api/v1/me/notifications").session(writerSession))
                .andExpect(jsonPath("$.items[0].postTitle").value("광고 글마당"))
                .andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].note").value("광고"));
        mvc.perform(get("/api/v1/editorial/actions").session(adminSession))
                .andExpect(jsonPath("$.items[0].action").value("removed"))
                .andExpect(jsonPath("$.items[0].title").value("광고 글마당"));
    }

    @Test
    void S3_운영_현황은_오늘_가입과_글을_센다() throws Exception {
        String before = mvc.perform(get("/api/v1/editorial/summary").session(adminSession))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        int membersBefore = JsonPath.read(before, "$.members");
        register("es");
        mvc.perform(get("/api/v1/editorial/summary").session(adminSession))
                .andExpect(jsonPath("$.members").value(membersBefore + 1));
    }
}
