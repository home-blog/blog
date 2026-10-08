package com.myblog.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.post.PostDeletingEvent;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.user.domain.User;
import com.myblog.user.service.MemberRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 삭제·탈퇴 때 댓글 정리 (specs/005 US2, T023, quickstart S-11의 댓글 줄, S-5, FR-006, FR-007, SC-005).
 * 한 묶음(실패하면 모두 취소)을 보려면 트랜잭션이 실제로 끝나야 하므로 클래스 전체 @Transactional을 쓰지 않는다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, CommentCleanupTest.Listeners.class})
class CommentCleanupTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FailingListener failing;

    @Autowired
    private MemberRegistration registration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private final List<Long> extraUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        failing.failWith.set(null);
    }

    @AfterEach
    void cleanUp() {
        failing.failWith.set(null);
        created.forEach(testComments::deleteMember);
        extraUsers.forEach(id -> {
            jdbc.update("delete from category where blog_id in (select blog_id from blog where users_id = ?)", id);
            jdbc.update("delete from blog where users_id = ?", id);
            jdbc.update("delete from users where users_id = ?", id);
        });
    }

    @Test
    void S11_글을_지우면_그_글의_댓글이_모두_지워진다() throws Exception {
        Member owner = register("o");
        Member reader = register("r");
        MockHttpSession ownerSession = members.login(mvc, owner);
        Long postId = members.addPost(owner.defaultCategoryId(), "지울 글", "public");
        Long keep = members.addPost(owner.defaultCategoryId(), "남길 글", "public");
        comment(members.login(mvc, reader), postId);
        comment(ownerSession, postId);
        testComments.rewind(owner.id());
        comment(ownerSession, keep);

        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());
        assertThat(commentsOn(postId)).isZero();
        assertThat(commentsOn(keep)).isOne();
    }

    @Test
    void S11_딸린_것을_지우다_실패하면_글과_댓글이_모두_그대로() throws Exception {
        Member owner = register("o");
        MockHttpSession ownerSession = members.login(mvc, owner);
        Long postId = members.addPost(owner.defaultCategoryId(), "실패할 글", "public");
        comment(ownerSession, postId);
        failing.failWith.set(new IllegalStateException("다른 모듈이 지우다 실패"));

        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession))
                .andExpect(status().isInternalServerError());
        assertThat(jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, postId)).isOne();
        assertThat(commentsOn(postId)).isOne();
    }

    @Test
    void S5_탈퇴하면_남의_글에_단_댓글은_남아_탈퇴한_사용자로_보이고_내_글의_댓글은_글과_함께_지워진다() throws Exception {
        Member a = register("a");
        Member b = register("b");
        Member c = register("c");
        MockHttpSession cSession = members.login(mvc, c);
        Long postOfA = members.addPost(a.defaultCategoryId(), "A의 글", "public");
        Long postOfC = members.addPost(c.defaultCategoryId(), "C의 글", "public");
        comment(cSession, postOfA);
        comment(members.login(mvc, b), postOfC);
        String oldNickname = c.user().getNickname();

        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(cSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        // 남의 글(A)에 단 C의 댓글은 남는다 (FR-007, 002 D-1)
        assertThat(commentsOn(postOfA)).isOne();
        // C의 블로그 글은 지워지고, 거기 B가 단 댓글도 함께 지워진다 (BlogClosingEvent → PostDeletingEvent)
        assertThat(jdbc.queryForObject("select count(*) from comment where post_id = ?", Long.class, postOfC)).isZero();

        // 새 회원이 C의 옛 닉네임으로 가입해도 C의 댓글은 "탈퇴한 사용자"
        User newcomer = registration.register("n" + System.nanoTime() + "@example.com",
                passwordEncoder.encode(TestMembers.PASSWORD), oldNickname);
        extraUsers.add(newcomer.getId());
        mvc.perform(get("/api/posts/{id}/comments", postOfA))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.comments[0].author.withdrawn").value(true))
                .andExpect(jsonPath("$.comments[0].author.nickname").value(nullValue()))
                .andExpect(jsonPath("$.comments[0].author.id").value(nullValue()));
    }

    private void comment(MockHttpSession session, Long postId) throws Exception {
        mvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "댓글")))
                .andExpect(status().isCreated());
    }

    private long commentsOn(Long postId) {
        return jdbc.queryForObject("select count(*) from comment where post_id = ?", Long.class, postId);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    /** 글을 지울 때 다른 모듈이 실패하는 경우를 흉내 낸다. failWith가 있으면 그 예외를 던진다. */
    static class FailingListener {

        final AtomicReference<RuntimeException> failWith = new AtomicReference<>();

        @EventListener
        void on(PostDeletingEvent event) {
            RuntimeException failure = failWith.get();
            if (failure != null) {
                throw failure;
            }
        }
    }

    @TestConfiguration
    static class Listeners {

        @Bean
        FailingListener failingListener() {
            return new FailingListener();
        }
    }
}
