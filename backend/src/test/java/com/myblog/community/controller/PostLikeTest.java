package com.myblog.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 좋아요 (specs/005 US3, T028, quickstart S-6, S-11의 좋아요 줄, FR-009 ~ FR-012, FR-028, SC-002, SC-005).
 * 동시 요청이 각자 커밋해야 하므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class PostLikeTest {

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
    private Member reader;
    private MockHttpSession ownerSession;
    private MockHttpSession readerSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        reader = register("r");
        ownerSession = members.login(mvc, owner);
        readerSession = members.login(mvc, reader);
        postId = members.addPost(owner.defaultCategoryId(), "좋아요 받을 글", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S6_1_2_누르면_수가_오르고_다시_취소하면_내려간다() throws Exception {
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(get("/api/posts/{id}", postId).session(readerSession))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));

        mvc.perform(delete("/api/posts/{id}/like", postId).with(csrf()).session(readerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
        assertThat(likes()).isZero();
    }

    @Test
    void S6_3_누르기를_여러_번_보내도_줄은_하나이고_누르지_않은_채_취소해도_200() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.likeCount").value(1));
        }
        assertThat(likes()).isOne();
        mvc.perform(delete("/api/posts/{id}/like", postId).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(delete("/api/posts/{id}/like", postId).with(csrf()).session(readerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void S6_3_동시에_5번_눌러도_줄은_하나() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession));
        }
        List<Integer> statuses = together(requests);
        assertThat(statuses).containsOnly(200);
        assertThat(likes()).isOne();
    }

    @Test
    void S6_4_자기_글은_403이고_줄이_생기지_않는다() throws Exception {
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(ownerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELF_LIKE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("※ 자기 글에는 좋아요를 누를 수 없습니다"));
        assertThat(likes()).isZero();
    }

    @Test
    void S6_5_로그인하지_않으면_401이고_CSRF가_없으면_거절() throws Exception {
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(put("/api/posts/{id}/like", postId).session(readerSession)).andExpect(status().isForbidden());
        assertThat(likes()).isZero();
    }

    @Test
    void S6_8_남의_비공개_글은_없는_글과_같은_404() throws Exception {
        Long hidden = members.addPost(owner.defaultCategoryId(), "비공개", "private");
        String missing = mvc.perform(put("/api/posts/{id}/like", 99_999_999L).with(csrf()).session(readerSession))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String others = mvc.perform(put("/api/posts/{id}/like", hidden).with(csrf()).session(readerSession))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(others).isEqualTo(missing).contains("POST_NOT_FOUND");
        mvc.perform(delete("/api/posts/{id}/like", hidden).with(csrf()).session(readerSession)).andExpect(status().isNotFound());
    }

    @Test
    void S11_글을_지우면_그_글의_좋아요가_모두_지워진다() throws Exception {
        Member second = register("s");
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(members.login(mvc, second)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());
        assertThat(likes()).isZero();
    }

    @Test
    void SC005_탈퇴하면_그_회원이_남의_글에_누른_좋아요가_모두_지워진다() throws Exception {
        Member other = register("t");
        Long otherPost = members.addPost(other.defaultCategoryId(), "다른 글", "public");
        mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(put("/api/posts/{id}/like", otherPost).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(put("/api/posts/{id}/like", otherPost).with(csrf()).session(ownerSession)).andExpect(status().isOk());

        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from post_like where users_id = ?", Long.class, reader.id())).isZero();
        // 남이 누른 좋아요는 그대로
        assertThat(jdbc.queryForObject("select count(*) from post_like where post_id = ?", Long.class, otherPost)).isOne();
    }

    @Test
    void 탈퇴가_진행_중일_때_온_좋아요는_탈퇴가_끝난_뒤_401이고_줄이_남지_않는다() throws Exception {
        // 탈퇴처럼 회원 줄을 FOR UPDATE로 잡고 있는 트랜잭션 (PR #30 리뷰)
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> withdrawal = pool.submit(() -> tx.executeWithoutResult(status -> {
                jdbc.queryForList("select 1 from users where users_id = ? for update", reader.id());
                locked.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                jdbc.update("update users set deleted_at = now() where users_id = ?", reader.id());
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Integer> like = pool.submit(() -> mvc.perform(put("/api/posts/{id}/like", postId).with(csrf())
                    .session(readerSession)).andReturn().getResponse().getStatus());
            Thread.sleep(500);
            assertThat(like.isDone()).as("탈퇴가 끝날 때까지 기다린다").isFalse();
            release.countDown();
            withdrawal.get(10, TimeUnit.SECONDS);
            assertThat(like.get(10, TimeUnit.SECONDS)).isEqualTo(401);
        } finally {
            release.countDown();
            pool.shutdown();
        }
        assertThat(likes()).isZero();
    }

    private List<Integer> together(List<RequestBuilder> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (RequestBuilder request : requests) {
                futures.add(pool.submit((Callable<Integer>) () -> {
                    start.await();
                    return mvc.perform(request).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdown();
        }
    }

    private long likes() {
        return jdbc.queryForObject("select count(*) from post_like where post_id = ?", Long.class, postId);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
