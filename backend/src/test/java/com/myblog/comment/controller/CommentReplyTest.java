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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * 답글·비밀 댓글 (specs/019 US1·US2, S1-1 ~ S2-3). 우편함 소식은 커밋 뒤 따로 만들어지므로 클래스 전체 @Transactional을
 * 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentReplyTest {

    private static final long WAIT_MS = 5000;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private Member asker;
    private Member other;
    private MockHttpSession ownerSession;
    private MockHttpSession askerSession;
    private MockHttpSession otherSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("ro");
        asker = register("ra");
        other = register("rx");
        ownerSession = members.login(mvc, owner);
        askerSession = members.login(mvc, asker);
        otherSession = members.login(mvc, other);
        postId = members.addPost(owner.defaultCategoryId(), "답글 달 글", "public");
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

    private ResultActions write(MockHttpSession session, Long target, Object... fields) throws Exception {
        Object[] all = new Object[fields.length + 2];
        all[0] = "body";
        all[1] = "댓글 " + System.nanoTime();
        System.arraycopy(fields, 0, all, 2, fields.length);
        return mvc.perform(post("/api/v1/posts/{id}/comments", target).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of(all)));
    }

    private Long writeOk(MockHttpSession session, Member who, Object... fields) throws Exception {
        testComments.rewind(who.id());
        String response = write(session, postId, fields).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions remove(MockHttpSession session, Long commentId) throws Exception {
        return mvc.perform(delete("/api/v1/comments/{id}", commentId).with(csrf()).session(session));
    }

    private String list(MockHttpSession session) throws Exception {
        var request = get("/api/v1/posts/{id}/comments", postId);
        if (session != null) {
            request.session(session);
        }
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long rows(Long commentId) {
        return jdbc.queryForObject("select count(*) from comment where comment_id = ?", Long.class, commentId);
    }

    private long notices(Member member, String kind) {
        return jdbc.queryForObject("select count(*) from notification where users_id = ? and kind = ?", Long.class,
                member.id(), kind);
    }

    private void awaitNotices(Member member, String kind, long expected) throws InterruptedException {
        long until = System.currentTimeMillis() + WAIT_MS;
        while (notices(member, kind) < expected && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
        assertThat(notices(member, kind)).isEqualTo(expected);
    }

    /** 소식 일꾼은 한 줄로 돈다: 표시용 댓글 소식이 생기면 그 앞의 일은 끝난 것이다. */
    private void settle() throws Exception {
        Member marker = register("rm");
        Long markerPost = members.addPost(marker.defaultCategoryId(), "일꾼 확인용", "public");
        testComments.rewind(other.id());
        write(otherSession, markerPost).andExpect(status().isCreated());
        awaitNotices(marker, "comment", 1);
    }

    @Test
    void S1_답글은_원댓글_아래에_붙고_답글의_답글도_같은_원댓글을_가리킨다() throws Exception {
        // 시각 되돌리기(rewind)가 순서를 바꾸지 않게 회원마다 한 번씩만 쓴다
        Member fourth = register("r4");
        MockHttpSession fourthSession = members.login(mvc, fourth);
        Long first = writeOk(askerSession, asker);
        Long second = writeOk(otherSession, other);
        Long reply = writeOk(ownerSession, owner, "parentId", first);
        Long replyOfReply = writeOk(fourthSession, fourth, "parentId", reply);

        assertThat(jdbc.queryForObject("select parent_id from comment where comment_id = ?", Long.class, replyOfReply))
                .isEqualTo(first);
        String body = list(null);
        List<Integer> ids = JsonPath.read(body, "$.comments[*].id");
        assertThat(ids.stream().map(Integer::longValue).toList()).containsExactly(first, reply, replyOfReply, second);
        assertThat((Object) JsonPath.read(body, "$.comments[0].parentId")).isNull();
        assertThat(((Number) JsonPath.read(body, "$.comments[1].parentId")).longValue()).isEqualTo(first);
        assertThat((Integer) JsonPath.read(body, "$.count")).isEqualTo(4);
        // 로그인하지 않았으면 답글 단추가 없다
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].canReply")).isFalse();
        assertThat((Boolean) JsonPath.read(list(otherSession), "$.comments[0].canReply")).isTrue();
    }

    @Test
    void S1_1_없는_댓글_다른_글의_댓글_지운_칸에는_답글을_달_수_없다() throws Exception {
        Long otherPost = members.addPost(owner.defaultCategoryId(), "다른 글", "public");
        testComments.rewind(asker.id());
        String response = write(askerSession, otherPost).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long elsewhere = ((Number) JsonPath.read(response, "$.id")).longValue();

        testComments.rewind(other.id());
        write(otherSession, postId, "parentId", 999_999_999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        write(otherSession, postId, "parentId", elsewhere).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        Long parent = writeOk(askerSession, asker);
        writeOk(ownerSession, owner, "parentId", parent);
        remove(askerSession, parent).andExpect(status().isNoContent());
        testComments.rewind(other.id());
        write(otherSession, postId, "parentId", parent).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void S1_2_답글이_남은_댓글을_지우면_지운_칸이_되고_마지막_답글이_지워지면_함께_사라진다() throws Exception {
        Long parent = writeOk(askerSession, asker);
        Long reply1 = writeOk(ownerSession, owner, "parentId", parent);
        Long reply2 = writeOk(otherSession, other, "parentId", parent);

        remove(askerSession, parent).andExpect(status().isNoContent());
        assertThat(rows(parent)).isEqualTo(1);
        String body = list(askerSession);
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].deleted")).isTrue();
        assertThat((String) JsonPath.read(body, "$.comments[0].body")).isEmpty();
        assertThat((Object) JsonPath.read(body, "$.comments[0].author.nickname")).isNull();
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].canDelete")).isFalse();
        assertThat((Boolean) JsonPath.read(body, "$.comments[0].canReply")).isFalse();
        assertThat((Integer) JsonPath.read(body, "$.count")).isEqualTo(2);
        // 지운 칸은 다시 지울 수 없다
        remove(askerSession, parent).andExpect(status().isNotFound());

        remove(ownerSession, reply1).andExpect(status().isNoContent());
        assertThat(rows(parent)).isEqualTo(1);
        remove(otherSession, reply2).andExpect(status().isNoContent());
        assertThat(rows(parent)).isZero();
        mvc.perform(get("/api/v1/posts/{id}/comments", postId)).andExpect(jsonPath("$.comments", hasSize(0)));
    }

    @Test
    void S1_3_지운_칸은_댓글_수와_블로그_관리_목록에_들어가지_않는다() throws Exception {
        Long parent = writeOk(askerSession, asker);
        writeOk(otherSession, other, "parentId", parent, "secret", true);
        remove(askerSession, parent).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/posts/{id}", postId)).andExpect(jsonPath("$.commentCount").value(1));
        String manage = mvc.perform(get("/api/v1/manage/comments").session(ownerSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(manage, "$.totalCount")).isEqualTo(1);
        assertThat((Boolean) JsonPath.read(manage, "$.items[0].reply")).isTrue();
        assertThat((Boolean) JsonPath.read(manage, "$.items[0].secret")).isTrue();
        mvc.perform(get("/api/v1/manage/comments/new-count").session(ownerSession))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void S1_4_답글_둘을_동시에_지워도_빈_지운_칸이_남지_않는다() throws Exception {
        for (int round = 0; round < 5; round++) {
            Long parent = writeOk(askerSession, asker);
            Long reply1 = writeOk(ownerSession, owner, "parentId", parent);
            Long reply2 = writeOk(otherSession, other, "parentId", parent);
            remove(askerSession, parent).andExpect(status().isNoContent());

            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Callable<Integer> a = () -> {
                    start.await();
                    return remove(ownerSession, reply1).andReturn().getResponse().getStatus();
                };
                Callable<Integer> b = () -> {
                    start.await();
                    return remove(otherSession, reply2).andReturn().getResponse().getStatus();
                };
                Future<Integer> fa = pool.submit(a);
                Future<Integer> fb = pool.submit(b);
                start.countDown();
                assertThat(fa.get()).isEqualTo(204);
                assertThat(fb.get()).isEqualTo(204);
            } finally {
                pool.shutdown();
            }
            assertThat(rows(parent)).as("빈 지운 칸").isZero();
        }
    }

    @Test
    void S1_4_원댓글_지우기와_답글_쓰기가_겹쳐도_답글은_지운_칸이나_404가_된다() throws Exception {
        for (int round = 0; round < 5; round++) {
            Long parent = writeOk(askerSession, asker);
            testComments.rewind(other.id());
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Integer> del = pool.submit(() -> {
                    start.await();
                    return remove(askerSession, parent).andReturn().getResponse().getStatus();
                });
                Future<Integer> reply = pool.submit(() -> {
                    start.await();
                    return write(otherSession, postId, "parentId", parent).andReturn().getResponse().getStatus();
                });
                start.countDown();
                assertThat(del.get()).isEqualTo(204);
                int replied = reply.get();
                if (replied == 201) {
                    // 답글이 먼저: 원댓글은 지운 칸으로 남는다
                    assertThat(jdbc.queryForObject("select deleted_at is not null from comment where comment_id = ?",
                            Boolean.class, parent)).isTrue();
                } else {
                    assertThat(replied).isEqualTo(404);
                    assertThat(rows(parent)).isZero();
                }
            } finally {
                pool.shutdown();
            }
        }
    }

    @Test
    void S1_4_답글이_잠금을_기다리는_사이_원댓글이_지운_칸이_되면_404다() throws Exception {
        Long parent = writeOk(askerSession, asker);
        writeOk(ownerSession, owner, "parentId", parent);
        testComments.rewind(other.id());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 지우는 쪽: 원댓글 줄을 잡고 지운 칸으로 만든 뒤, 답글 요청이 잠금에 걸릴 때까지 커밋하지 않는다
            Future<?> eraser = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                jdbc.queryForList("select comment_id from comment where comment_id = ? for update", parent);
                jdbc.update("update comment set body = '', deleted_at = now() where comment_id = ?", parent);
                locked.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            locked.await();
            Future<Integer> reply = pool.submit(() ->
                    write(otherSession, postId, "parentId", parent).andReturn().getResponse().getStatus());
            Thread.sleep(500);
            release.countDown();
            eraser.get();
            assertThat(reply.get()).isEqualTo(404);
        } finally {
            release.countDown();
            pool.shutdown();
        }
        assertThat(jdbc.queryForObject("select count(*) from comment where parent_id = ?", Long.class, parent))
                .isEqualTo(1);
    }

    @Test
    void S1_5_답글이_달리면_원댓글_쓴_사람에게_답글_소식이_가고_자기_답글에는_가지_않는다() throws Exception {
        Long askerComment = writeOk(askerSession, asker);
        writeOk(otherSession, other, "parentId", askerComment);
        awaitNotices(asker, "reply", 1);
        awaitNotices(owner, "comment", 2);

        // 자기 댓글에 자기가 단 답글: 원댓글 쓴 사람에게는 가지 않는다
        writeOk(askerSession, asker, "parentId", askerComment);
        // 글 주인의 댓글에 단 답글: 글 주인은 댓글 소식 대신 답글 소식 하나
        Long ownerComment = writeOk(ownerSession, owner);
        writeOk(askerSession, asker, "parentId", ownerComment);
        settle();
        assertThat(notices(asker, "reply")).isEqualTo(1);
        assertThat(notices(owner, "reply")).isEqualTo(1);
        assertThat(notices(owner, "comment")).isEqualTo(3);

        String mailbox = mvc.perform(get("/api/v1/me/notifications").session(askerSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(mailbox, "$.items[0].kind")).isEqualTo("reply");
        assertThat(((Number) JsonPath.read(mailbox, "$.items[0].postId")).longValue()).isEqualTo(postId);
    }

    @Test
    void S2_1_비밀_댓글은_쓴_사람과_블로그_주인만_읽고_다른_사람에게는_내용이_실리지_않는다() throws Exception {
        writeOk(askerSession, asker, "secret", true);

        for (MockHttpSession reader : new MockHttpSession[] {askerSession, ownerSession}) {
            String body = list(reader);
            assertThat((Boolean) JsonPath.read(body, "$.comments[0].secret")).isTrue();
            assertThat((Boolean) JsonPath.read(body, "$.comments[0].sealed")).isFalse();
            assertThat((String) JsonPath.read(body, "$.comments[0].body")).startsWith("댓글 ");
        }
        for (MockHttpSession reader : new MockHttpSession[] {otherSession, null}) {
            String body = list(reader);
            assertThat((Boolean) JsonPath.read(body, "$.comments[0].sealed")).isTrue();
            assertThat((String) JsonPath.read(body, "$.comments[0].body")).isEmpty();
            assertThat((Object) JsonPath.read(body, "$.comments[0].author.id")).isNull();
            assertThat((Object) JsonPath.read(body, "$.comments[0].author.nickname")).isNull();
            assertThat(body).doesNotContain(asker.user().getNickname());
            assertThat((Integer) JsonPath.read(body, "$.count")).isEqualTo(1);
        }
    }

    @Test
    void S2_2_비밀_댓글의_답글은_저절로_비밀이고_원댓글_쓴_사람도_읽으며_못_읽는_사람은_답글을_달_수_없다() throws Exception {
        Long secret = writeOk(askerSession, asker, "secret", true);
        Long answer = writeOk(ownerSession, owner, "parentId", secret);
        assertThat(jdbc.queryForObject("select is_secret from comment where comment_id = ?", Boolean.class, answer))
                .isTrue();

        String askerView = list(askerSession);
        assertThat((Boolean) JsonPath.read(askerView, "$.comments[1].sealed")).isFalse();
        String otherView = list(otherSession);
        assertThat((Boolean) JsonPath.read(otherView, "$.comments[1].sealed")).isTrue();
        assertThat((Boolean) JsonPath.read(otherView, "$.comments[0].canReply")).isFalse();

        testComments.rewind(other.id());
        write(otherSession, postId, "parentId", secret).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void S2_3_블로그_관리에서_주인은_비밀_댓글을_읽는다() throws Exception {
        writeOk(askerSession, asker, "secret", true);
        String manage = mvc.perform(get("/api/v1/manage/comments").session(ownerSession)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((Boolean) JsonPath.read(manage, "$.items[0].secret")).isTrue();
        assertThat((Boolean) JsonPath.read(manage, "$.items[0].reply")).isFalse();
        assertThat((String) JsonPath.read(manage, "$.items[0].preview")).startsWith("댓글 ");
    }

    @Test
    void 글을_지우면_답글이_달린_댓글도_한꺼번에_지워진다() throws Exception {
        Long parent = writeOk(askerSession, asker);
        Long reply = writeOk(otherSession, other, "parentId", parent);
        mvc.perform(delete("/api/v1/posts/{id}", postId).with(csrf()).session(ownerSession))
                .andExpect(status().isNoContent());
        assertThat(rows(parent) + rows(reply)).isZero();
    }
}
