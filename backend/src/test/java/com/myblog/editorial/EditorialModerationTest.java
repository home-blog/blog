package com.myblog.editorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Duration;
import java.time.Instant;
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
 * 편집실 운영 강화 (specs/022): 댓글 신고 처리(S1-4), 회원 정지(S2-1 ~ S2-5), 운영 현황(US3).
 * 우편함 소식은 커밋 뒤 따로 만들어지므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
class EditorialModerationTest {

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
    private Member owner;
    private Member writer;
    private Member admin;
    private MockHttpSession ownerSession;
    private MockHttpSession writerSession;
    private MockHttpSession adminSession;
    private Long postId;
    private Instant start;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        start = LocalDate.of(2100, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1, 300_000)).atTime(3, 0)
                .toInstant(ZoneOffset.UTC);
        clock.set(start);
        owner = register("mo");
        writer = register("mw");
        admin = register("ma");
        jdbc.update("update users set role = 'admin' where users_id = ?", admin.id());
        ownerSession = members.login(mvc, owner);
        writerSession = members.login(mvc, writer);
        adminSession = members.login(mvc, admin);
        postId = members.addPost(owner.defaultCategoryId(), "편집실 댓글 글", "public");
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        for (Member member : created) {
            jdbc.update("delete from notification where users_id = ?", member.id());
            jdbc.update("delete from editorial_action where users_id = ? or member_id = ?", member.id(), member.id());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private Long writeComment() throws Exception {
        testComments.rewind(writer.id());
        String response = mvc.perform(post("/api/v1/posts/{id}/comments", postId).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "광고 댓글입니다")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void reportBy(Long commentId, int people) throws Exception {
        for (int i = 0; i < people; i++) {
            MockHttpSession session = members.login(mvc, register("mr"));
            mvc.perform(post("/api/v1/comments/{id}/reports", commentId).with(csrf()).session(session)
                            .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "SPAM")))
                    .andExpect(status().isCreated());
        }
    }

    private ResultActions adminPost(String path, Object id, Object... body) throws Exception {
        return mvc.perform(post(path, id).with(csrf()).session(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of(body)));
    }

    private ResultActions login(Member member, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("email", member.user().getEmail(), "password", password)));
    }

    private boolean hidden(Long commentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select hidden_at is not null from comment where comment_id = ?",
                Boolean.class, commentId));
    }

    private long actions(String kind, String action, Long target) {
        String column = "comment".equals(kind) ? "comment_id" : "member_id";
        return jdbc.queryForObject("select count(*) from editorial_action where target_kind = ? and action = ? and "
                + column + " = ?", Long.class, kind, action, target);
    }

    private String awaitNoticeTitle(Member member) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            List<String> titles = jdbc.queryForList(
                    "select post_title from notification where users_id = ? and kind = 'editorial'", String.class,
                    member.id());
            if (!titles.isEmpty()) {
                return titles.get(0);
            }
            Thread.sleep(50);
        }
        return null;
    }

    /* S1-4 댓글 신고 처리 */

    @Test
    void S1_4_관리자가_아니면_403_로그인하지_않으면_401() throws Exception {
        Long commentId = writeComment();
        mvc.perform(get("/api/v1/editorial/reports/comments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/editorial/reports/comments").session(ownerSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/editorial/members").session(ownerSession)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/editorial/comments/{id}/hide", commentId).with(csrf()).session(ownerSession)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "x")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/editorial/members/{id}/suspend", writer.id()).with(csrf()).session(ownerSession)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("days", 1, "reason", "x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void S1_4_저절로_가린_댓글은_대기에_오르고_반려하면_다시_보인다() throws Exception {
        Long commentId = writeComment();
        reportBy(commentId, 3);
        assertThat(hidden(commentId)).isTrue();

        mvc.perform(get("/api/v1/editorial/reports/comments/{id}", commentId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.postTitle").value("편집실 댓글 글"))
                .andExpect(jsonPath("$.body").value("광고 댓글입니다"))
                .andExpect(jsonPath("$.writerNickname").value(writer.user().getNickname()))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.hiddenBy").value("auto"))
                .andExpect(jsonPath("$.reports.length()").value(3))
                .andExpect(jsonPath("$.reports[0].reason").value("SPAM"));

        String list = mvc.perform(get("/api/v1/editorial/reports/comments").session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> counts = JsonPath.read(list, "$.items[?(@.commentId == " + commentId + ")].reportCount");
        assertThat(counts).containsExactly(3);
        List<String> excerpts = JsonPath.read(list, "$.items[?(@.commentId == " + commentId + ")].excerpt");
        assertThat(excerpts).containsExactly("광고 댓글입니다");

        adminPost("/api/v1/editorial/comments/{id}/dismiss", commentId).andExpect(status().isNoContent());
        assertThat(hidden(commentId)).isFalse();
        assertThat(actions("comment", "dismissed", commentId)).isEqualTo(1);
        adminPost("/api/v1/editorial/comments/{id}/dismiss", commentId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_RESOLVED"));
        // 처리 뒤 새 신고는 다시 대기에 오른다
        reportBy(commentId, 1);
        mvc.perform(get("/api/v1/editorial/reports/comments/{id}", commentId).session(adminSession))
                .andExpect(jsonPath("$.reports.length()").value(1));
    }

    @Test
    void S1_4_가리기는_신고를_닫고_쓴_사람에게_알리며_되돌릴_수_있다() throws Exception {
        Long commentId = writeComment();
        reportBy(commentId, 1);
        adminPost("/api/v1/editorial/comments/{id}/hide", commentId, "reason", "  ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EDITORIAL_REASON_INVALID"));
        adminPost("/api/v1/editorial/comments/{id}/hide", commentId, "reason", "광고 댓글")
                .andExpect(status().isNoContent());
        assertThat(hidden(commentId)).isTrue();
        assertThat(jdbc.queryForObject("select hidden_by from comment where comment_id = ?", String.class, commentId))
                .isEqualTo("editor");
        assertThat(jdbc.queryForObject("select count(*) from comment_report where comment_id = ? and resolution = 'hidden'",
                Long.class, commentId)).isEqualTo(1);
        assertThat(actions("comment", "hidden", commentId)).isEqualTo(1);
        assertThat(awaitNoticeTitle(writer)).isEqualTo("‘편집실 댓글 글’에 남긴 댓글");
        adminPost("/api/v1/editorial/comments/{id}/hide", commentId, "reason", "또").andExpect(status().isConflict());

        String history = mvc.perform(get("/api/v1/editorial/actions").session(adminSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> kinds = JsonPath.read(history, "$.items[?(@.commentId == " + commentId + ")].targetKind");
        assertThat(kinds).containsExactly("comment");

        adminPost("/api/v1/editorial/comments/{id}/restore", commentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.restored").value(true));
        assertThat(hidden(commentId)).isFalse();
        adminPost("/api/v1/editorial/comments/{id}/restore", commentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.restored").value(false));
        assertThat(actions("comment", "restored", commentId)).isEqualTo(1);
        adminPost("/api/v1/editorial/comments/{id}/restore", 999_999_999L).andExpect(status().isNotFound());
    }

    /* US2 회원 정지 */

    @Test
    void S2_1_관리자_자신_없는_회원_탈퇴한_회원은_정지할_수_없고_기간과_사유를_검사한다() throws Exception {
        adminPost("/api/v1/editorial/members/{id}/suspend", admin.id(), "days", 7, "reason", "x")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_NOT_SUSPENDABLE"));
        Member otherAdmin = register("mb");
        jdbc.update("update users set role = 'admin' where users_id = ?", otherAdmin.id());
        adminPost("/api/v1/editorial/members/{id}/suspend", otherAdmin.id(), "days", 7, "reason", "x")
                .andExpect(status().isConflict());
        adminPost("/api/v1/editorial/members/{id}/suspend", 999_999_999L, "days", 7, "reason", "x")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
        Member gone = register("mg");
        jdbc.update("update users set deleted_at = now() where users_id = ?", gone.id());
        adminPost("/api/v1/editorial/members/{id}/suspend", gone.id(), "days", 7, "reason", "x")
                .andExpect(status().isNotFound());
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 2, "reason", "x")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUSPEND_DAYS_INVALID"));
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 7, "reason", "가".repeat(201))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("EDITORIAL_REASON_INVALID"));
        assertThat(jdbc.queryForObject("select suspended_until from users where users_id = ?", Instant.class,
                writer.id())).isNull();
    }

    @Test
    void S2_2_S2_3_정지하면_세션과_열쇠가_끊기고_비밀번호가_맞을_때만_정지를_알린다() throws Exception {
        mvc.perform(post("/api/v1/me/access-keys").with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("name", "노트북", "scope", "read", "days", 30)))
                .andExpect(status().isCreated());
        MockHttpSession second = members.login(mvc, writer);

        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 7, "reason", "광고 댓글 반복")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspendedUntil").value(start.plus(Duration.ofDays(7)).toString()));

        // 두 번째 벽: 세션 저장소에서 지워졌더라도, 이 쿠키를 들고 오면 로그인하지 않은 것
        mvc.perform(get("/api/v1/auth/me").session(writerSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").session(second)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from spring_session where principal_name = ?", Long.class,
                String.valueOf(writer.id()))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from access_key where users_id = ?", Long.class, writer.id()))
                .isZero();

        login(writer, "wrong-password1!").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(writer, TestMembers.PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("광고 댓글 반복")));
        assertThat(jdbc.queryForObject("select count(*) from spring_session where principal_name = ?", Long.class,
                String.valueOf(writer.id()))).isZero();

        String list = mvc.perform(get("/api/v1/editorial/members").param("q", writer.user().getNickname())
                        .session(adminSession)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(list, "$.totalCount")).isEqualTo(1);
        assertThat((Boolean) JsonPath.read(list, "$.items[0].suspended")).isTrue();
        assertThat((String) JsonPath.read(list, "$.items[0].suspendReason")).isEqualTo("광고 댓글 반복");
        assertThat(((Number) JsonPath.read(list, "$.items[0].blogId")).longValue()).isEqualTo(writer.blogId());
        assertThat((List<Integer>) JsonPath.read(list, "$.suspendDays")).containsExactly(1, 3, 7, 30, 365);
        assertThat(actions("member", "suspended", writer.id())).isEqualTo(1);
    }

    @Test
    void S2_4_다시_정지하면_기간을_새로_정하고_풀기는_한_번만_기록한다() throws Exception {
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 30, "reason", "첫 정지")
                .andExpect(status().isOk());
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 1, "reason", "줄임")
                .andExpect(jsonPath("$.suspendedUntil").value(start.plus(Duration.ofDays(1)).toString()));
        adminPost("/api/v1/editorial/members/{id}/unsuspend", writer.id()).andExpect(status().isOk())
                .andExpect(jsonPath("$.unsuspended").value(true));
        login(writer, TestMembers.PASSWORD).andExpect(status().isOk());
        adminPost("/api/v1/editorial/members/{id}/unsuspend", writer.id()).andExpect(status().isOk())
                .andExpect(jsonPath("$.unsuspended").value(false));
        adminPost("/api/v1/editorial/members/{id}/unsuspend", 999_999_999L).andExpect(status().isNotFound());
        assertThat(actions("member", "suspended", writer.id())).isEqualTo(2);
        assertThat(actions("member", "unsuspended", writer.id())).isEqualTo(1);
    }

    @Test
    void S2_5_정지는_그_시각이_되면_저절로_풀린다() throws Exception {
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 1, "reason", "하루").andExpect(status().isOk());
        clock.set(start.plus(Duration.ofDays(1)).minusSeconds(1));
        login(writer, TestMembers.PASSWORD).andExpect(status().isForbidden());
        clock.set(start.plus(Duration.ofDays(1)));
        login(writer, TestMembers.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 회원_찾기는_닉네임_일부로_찾고_퍼센트는_글자_그대로() throws Exception {
        String nickname = writer.user().getNickname();
        String part = nickname.substring(2, 6);
        String list = mvc.perform(get("/api/v1/editorial/members").param("q", part.toUpperCase()).session(adminSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(list, "$.items[*].memberId");
        assertThat(ids.stream().map(Integer::longValue)).contains(writer.id());
        mvc.perform(get("/api/v1/editorial/members").param("q", "%").session(adminSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/v1/editorial/members").param("q", "_").session(adminSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    /* US3 */

    @Test
    void US3_현황에_댓글_신고_대기와_정지_중_회원이_있다() throws Exception {
        String before = mvc.perform(get("/api/v1/editorial/summary").session(adminSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long pendingComments = ((Number) JsonPath.read(before, "$.pendingCommentReports")).longValue();
        long pendingAll = ((Number) JsonPath.read(before, "$.pendingReports")).longValue();
        long suspended = ((Number) JsonPath.read(before, "$.suspendedMembers")).longValue();

        reportBy(writeComment(), 2);
        adminPost("/api/v1/editorial/members/{id}/suspend", writer.id(), "days", 3, "reason", "x").andExpect(status().isOk());

        String after = mvc.perform(get("/api/v1/editorial/summary").session(adminSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(after, "$.pendingCommentReports")).longValue())
                .isGreaterThanOrEqualTo(pendingComments + 2);
        assertThat(((Number) JsonPath.read(after, "$.pendingReports")).longValue())
                .isGreaterThanOrEqualTo(pendingAll + 2);
        assertThat(((Number) JsonPath.read(after, "$.suspendedMembers")).longValue())
                .isGreaterThanOrEqualTo(suspended + 1);
    }
}
