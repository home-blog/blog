package com.myblog.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * 읽음 처리와 새 댓글이 겹칠 때 (specs/006 T031, quickstart S-5의 7, research R-4).
 * 댓글은 이번 목록에서 NEW이거나 다음번 새 댓글 수에 남는다. 보지도 못하고 읽음이 되는 경우는 0건이다.
 * 실제 트랜잭션끼리 겹쳐야 하므로 클래스 전체에 @Transactional을 걸지 않는다. 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentsReadRaceTest {

    private static final int ROUNDS = 20;

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
        postId = members.addPost(owner.defaultCategoryId(), "글", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S5_7_읽음_처리와_새_댓글이_겹쳐도_보지_못하고_사라지는_댓글이_없다() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                markRead(); // 이번 판은 0에서 시작
                testComments.rewind(writer.id());
                CountDownLatch start = new CountDownLatch(1);
                CompletableFuture<Long> written = CompletableFuture.supplyAsync(() -> {
                    await(start);
                    return write();
                }, pool);
                CompletableFuture<List<Long>> seenAsNew = CompletableFuture.supplyAsync(() -> {
                    await(start);
                    return openCommentManagement();
                }, pool);
                start.countDown();

                Long commentId = written.get();
                boolean shownAsNew = seenAsNew.get().contains(commentId);
                long nextCount = newCount();
                assertThat(shownAsNew || nextCount == 1)
                        .as("판 %d: 댓글 %d는 이번 목록의 NEW이거나 다음 새 댓글 수에 남아야 한다", round, commentId)
                        .isTrue();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 읽음_처리_두_개가_동시에_와도_시각이_뒤로_가지_않는다() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Instant last = null;
            for (int round = 0; round < 10; round++) {
                CountDownLatch start = new CountDownLatch(1);
                List<CompletableFuture<Void>> calls = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    calls.add(CompletableFuture.runAsync(() -> {
                        await(start);
                        markRead();
                    }, pool));
                }
                start.countDown();
                CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).get();
                Instant now = jdbc.queryForObject("select comments_read_at from blog where blog_id = ?", java.sql.Timestamp.class,
                        owner.blogId()).toInstant();
                if (last != null) {
                    assertThat(now).isAfterOrEqualTo(last);
                }
                last = now;
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** 화면이 댓글 관리를 열 때처럼: 읽음 처리 → 받은 이전 시각으로 첫 쪽을 읽어 NEW인 댓글 번호. */
    private List<Long> openCommentManagement() {
        try {
            String read = markRead();
            String previous = JsonPath.read(read, "$.previousReadAt");
            String list = mvc.perform(get("/api/manage/comments").param("newSince", previous == null ? "never" : previous)
                            .session(ownerSession))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<Number> ids = JsonPath.read(list, "$.items[?(@.isNew == true)].commentId");
            return ids.stream().map(Number::longValue).toList();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String markRead() {
        try {
            return mvc.perform(post("/api/manage/comments/read").with(csrf()).session(ownerSession))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Long write() {
        try {
            String body = mvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).session(writerSession)
                            .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "새 댓글")))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            return ((Number) JsonPath.read(body, "$.id")).longValue();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private long newCount() throws Exception {
        String body = mvc.perform(get("/api/manage/comments/new-count").session(ownerSession))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.count")).longValue();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
