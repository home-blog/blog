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
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * 댓글 신고와 자동 가림 (specs/022 US1, S1-1 ~ S1-3, S1-5). 가림 기준은 설정 moderation.comment.auto-hide-count(3건).
 * 신고는 커밋된 줄로 세므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentReportTest {

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
        owner = register("co");
        writer = register("cw");
        ownerSession = members.login(mvc, owner);
        writerSession = members.login(mvc, writer);
        postId = members.addPost(owner.defaultCategoryId(), "신고할 댓글이 달린 글", "public");
    }

    @AfterEach
    void cleanUp() {
        for (Member member : created) {
            jdbc.update("delete from notification where users_id = ?", member.id());
        }
        created.forEach(testComments::deleteMember);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private Long writeComment(MockHttpSession session, Member who, Object... fields) throws Exception {
        testComments.rewind(who.id());
        Object[] all = new Object[fields.length + 2];
        all[0] = "body";
        all[1] = "신고될 댓글 " + System.nanoTime();
        System.arraycopy(fields, 0, all, 2, fields.length);
        String response = mvc.perform(post("/api/v1/posts/{id}/comments", postId).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of(all)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions report(MockHttpSession session, Long commentId, String reason, String detail)
            throws Exception {
        var request = post("/api/v1/comments/{id}/reports", commentId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", reason, "detail", detail));
        if (session != null) {
            request.session(session);
        }
        return mvc.perform(request);
    }

    private MockHttpSession reporter() throws Exception {
        return members.login(mvc, register("cr"));
    }

    private String list(MockHttpSession session) throws Exception {
        var request = get("/api/v1/posts/{id}/comments", postId);
        if (session != null) {
            request.session(session);
        }
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private boolean hidden(Long commentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select hidden_at is not null from comment where comment_id = ?",
                Boolean.class, commentId));
    }

    @Test
    void S1_1_신고를_받으면_201이고_한_사람은_한_번만() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        MockHttpSession reporter = reporter();
        report(reporter, commentId, "SPAM", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다"))
                .andExpect(jsonPath("$.hidden").value(false));
        report(reporter, commentId, "ABUSE", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));
        // 기타 설명은 기타일 때만 남는다
        report(reporter(), commentId, "OTHER", "  광고 링크  ").andExpect(status().isCreated());
        assertThat(jdbc.queryForList("select detail from comment_report where comment_id = ? order by comment_report_id",
                String.class, commentId)).containsExactly(null, "광고 링크");
    }

    @Test
    void S1_1_로그인_안함_401_내_댓글_403_입력_400() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        report(null, commentId, "SPAM", null).andExpect(status().isUnauthorized());
        report(writerSession, commentId, "SPAM", null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELF_REPORT_NOT_ALLOWED"));
        MockHttpSession reporter = reporter();
        report(reporter, commentId, "spam", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
        report(reporter, commentId, "OTHER", "가".repeat(201)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("detail"));
        // 설명 200자는 된다 (경계)
        report(reporter, commentId, "OTHER", "가".repeat(200)).andExpect(status().isCreated());
    }

    @Test
    void S1_1_없는_댓글_지운_칸_봉한_댓글_볼_수_없는_글의_댓글은_404() throws Exception {
        MockHttpSession reporter = reporter();
        report(reporter, 999_999_999L, "SPAM", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        // 봉한 쪽지: 쓴 사람·주인만 읽는다
        Long secret = writeComment(writerSession, writer, "secret", true);
        report(reporter, secret, "SPAM", null).andExpect(status().isNotFound());
        report(ownerSession, secret, "SPAM", null).andExpect(status().isCreated());

        // 지운 칸: 답글이 남은 원댓글을 지우면 줄이 남는다
        Long root = writeComment(writerSession, writer);
        writeComment(ownerSession, owner, "parentId", root);
        mvc.perform(delete("/api/v1/comments/{id}", root).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        report(reporter, root, "SPAM", null).andExpect(status().isNotFound());

        // 비공개 글의 댓글
        Long visible = writeComment(writerSession, writer);
        jdbc.update("update post set visibility = 'private' where post_id = ?", postId);
        report(reporter, visible, "SPAM", null).andExpect(status().isNotFound());
    }

    @Test
    void S1_2_서로_다른_세_명이_신고하면_저절로_가리고_두_건이면_보인다() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        report(reporter(), commentId, "SPAM", null).andExpect(jsonPath("$.hidden").value(false));
        report(reporter(), commentId, "ABUSE", null).andExpect(jsonPath("$.hidden").value(false));
        assertThat(hidden(commentId)).isFalse();
        String two = list(null);
        assertThat((Boolean) JsonPath.read(two, "$.comments[0].hidden")).isFalse();

        report(reporter(), commentId, "SPAM", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.hidden").value(true));
        assertThat(jdbc.queryForObject("select hidden_by from comment where comment_id = ?", String.class, commentId))
                .isEqualTo("auto");
        // 네 번째 신고는 이미 가려진 댓글이라 404
        report(reporter(), commentId, "SPAM", null).andExpect(status().isNotFound());
    }

    @Test
    void S1_2_반려로_닫힌_신고는_세지_않는다() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        report(reporter(), commentId, "SPAM", null).andExpect(status().isCreated());
        report(reporter(), commentId, "SPAM", null).andExpect(status().isCreated());
        jdbc.update("update comment_report set resolved_at = now(), resolution = 'dismissed' where comment_id = ?",
                commentId);
        report(reporter(), commentId, "SPAM", null).andExpect(jsonPath("$.hidden").value(false));
        report(reporter(), commentId, "SPAM", null).andExpect(jsonPath("$.hidden").value(false));
        assertThat(hidden(commentId)).isFalse();
        report(reporter(), commentId, "SPAM", null).andExpect(jsonPath("$.hidden").value(true));
    }

    @Test
    void S1_2_동시에_신고가_몰려도_한_번만_가린다() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        List<MockHttpSession> sessions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sessions.add(reporter());
        }
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(sessions.size());
        try {
            List<Future<String>> results = new ArrayList<>();
            for (MockHttpSession session : sessions) {
                Callable<String> task = () -> {
                    start.await();
                    return report(session, commentId, "SPAM", null).andReturn().getResponse().getContentAsString();
                };
                results.add(pool.submit(task));
            }
            start.countDown();
            int hiddenNow = 0;
            int accepted = 0;
            for (Future<String> result : results) {
                String body = result.get();
                if (body.contains("\"hidden\":true")) {
                    hiddenNow++;
                }
                if (body.contains("신고가 접수되었습니다")) {
                    accepted++;
                }
            }
            assertThat(hiddenNow).isEqualTo(1);
            // 가려진 뒤에 줄을 선 요청은 404: 가린 뒤로는 신고가 쌓이지 않는다
            assertThat(accepted).isEqualTo(3);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from comment_report where comment_id = ?", Long.class,
                commentId)).isEqualTo(3);
    }

    @Test
    void S1_3_가린_댓글은_내용과_쓴_사람이_없고_답글을_못_달며_수에서_빠진다() throws Exception {
        Long root = writeComment(writerSession, writer);
        Long reply = writeComment(ownerSession, owner, "parentId", root);
        jdbc.update("update comment set hidden_at = now(), hidden_by = 'editor' where comment_id = ?", root);

        String asOwner = list(ownerSession);
        assertThat((Integer) JsonPath.read(asOwner, "$.count")).isEqualTo(1);
        assertThat((List<?>) JsonPath.read(asOwner, "$.comments")).hasSize(2);
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[0].hidden")).isTrue();
        assertThat((String) JsonPath.read(asOwner, "$.comments[0].body")).isEmpty();
        assertThat((Object) JsonPath.read(asOwner, "$.comments[0].author.nickname")).isNull();
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[0].canReply")).isFalse();
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[0].canReport")).isFalse();
        // 주인은 그래도 지울 수 있다
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[0].canDelete")).isTrue();
        // 답글은 그대로 보인다
        assertThat(((Number) JsonPath.read(asOwner, "$.comments[1].id")).longValue()).isEqualTo(reply);
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[1].hidden")).isFalse();

        // 쓴 사람도 내용은 보지 못하지만 지울 수 있다
        String asWriter = list(writerSession);
        assertThat((String) JsonPath.read(asWriter, "$.comments[0].body")).isEmpty();
        assertThat((Boolean) JsonPath.read(asWriter, "$.comments[0].canDelete")).isTrue();

        // 가린 댓글에 답글을 달면 없는 댓글과 같다
        testComments.rewind(owner.id());
        mvc.perform(post("/api/v1/posts/{id}/comments", postId).with(csrf()).session(ownerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "답", "parentId", root)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        // 글 상세의 댓글 수에서도 빠진다
        mvc.perform(get("/api/v1/posts/{id}", postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1));
        mvc.perform(delete("/api/v1/comments/{id}", root).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void S1_3_보이는_남의_댓글에만_신고_단추() throws Exception {
        writeComment(writerSession, writer);
        String asWriter = list(writerSession);
        assertThat((Boolean) JsonPath.read(asWriter, "$.comments[0].canReport")).isFalse();
        String asOwner = list(ownerSession);
        assertThat((Boolean) JsonPath.read(asOwner, "$.comments[0].canReport")).isTrue();
        String anonymous = list(null);
        assertThat((Boolean) JsonPath.read(anonymous, "$.comments[0].canReport")).isFalse();
    }

    @Test
    void S1_5_댓글을_지우면_신고도_함께_지운다() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        report(reporter(), commentId, "SPAM", null).andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/comments/{id}", commentId).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from comment_report where comment_id = ?", Long.class,
                commentId)).isZero();
    }

    @Test
    void 블로그_관리_댓글_목록은_가린_댓글을_가림으로_표시하고_미리보기를_비운다() throws Exception {
        Long commentId = writeComment(writerSession, writer);
        jdbc.update("update comment set hidden_at = now(), hidden_by = 'auto' where comment_id = ?", commentId);
        mvc.perform(get("/api/v1/manage/comments").session(ownerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].hidden").value(true))
                .andExpect(jsonPath("$.items[0].preview").value(""))
                .andExpect(jsonPath("$.items[0].author.nickname").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void S1_5_답글이_남아_지운_칸이_된_댓글의_신고도_지워_편집실_대기에_남지_않는다() throws Exception {
        Long rootId = writeComment(writerSession, writer);
        writeComment(ownerSession, owner, "parentId", rootId);
        report(reporter(), rootId, "SPAM", null).andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/comments/{id}", rootId).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select deleted_at is not null from comment where comment_id = ?",
                Boolean.class, rootId)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from comment_report where comment_id = ?", Long.class,
                rootId)).isZero();
    }

    @Test
    void S1_3_가린_비밀_댓글도_남에게는_봉한_쪽지가_아니라_가린_칸이다() throws Exception {
        Long commentId = writeComment(writerSession, writer, "secret", true);
        jdbc.update("update comment set hidden_at = now(), hidden_by = 'editor' where comment_id = ?", commentId);
        String body = list(reporter());
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].hidden")).isTrue();
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].sealed")).isFalse();
        assertThat((Integer) JsonPath.read(body, "$.count")).isZero();
    }
}
