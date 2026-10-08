package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.post.PostDeletingEvent;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 삭제 (specs/003 US4, quickstart S-8의 7·8, T031), 탈퇴하면 그 회원의 글이 모두 지워지는 것 (002 S-8, T036).
 * 한 묶음(실패하면 모두 취소)을 보려면 트랜잭션이 실제로 끝나야 하므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, PostDeleteTest.Listeners.class})
class PostDeleteTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DeletingListener listener;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private MockHttpSession ownerSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("d");
        ownerSession = members.login(mvc, owner);
        listener.heard.clear();
        listener.failWith.set(null);
    }

    @AfterEach
    void cleanUp() {
        listener.failWith.set(null);
        for (Member member : created) {
            jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", member.blogId());
            jdbc.update("delete from category where blog_id = ?", member.blogId());
            jdbc.update("delete from blog where blog_id = ?", member.blogId());
            jdbc.update("delete from users where users_id = ?", member.id());
        }
    }

    @Test
    void 내_글을_지우면_204이고_지우기_전에_PostDeletingEvent를_낸다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "지울 글", "public");
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());
        assertThat(count(postId)).isZero();
        assertThat(listener.heard).containsExactly(postId);
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNotFound());
    }

    @Test
    void S8_8_남의_글을_지우려_하면_404이고_그대로() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "남의 글", "public");
        MockHttpSession other = members.login(mvc, register("e"));
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        assertThat(count(postId)).isOne();
        assertThat(listener.heard).isEmpty();
    }

    @Test
    void S8_7_딸린_것을_지우다_실패하면_글도_그대로_남는다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "실패할 글", "public");
        listener.failWith.set(new IllegalStateException("댓글을 지우다 실패"));
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession))
                .andExpect(status().isInternalServerError());
        assertThat(count(postId)).isOne();
    }

    @Test
    void CSRF가_없으면_거절하고_로그인하지_않으면_401() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        mvc.perform(delete("/api/posts/{id}", postId).session(ownerSession)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf())).andExpect(status().isUnauthorized());
        assertThat(count(postId)).isOne();
    }

    @Test
    void T036_탈퇴하면_그_회원의_글이_모두_지워지고_글마다_PostDeletingEvent를_낸다() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "private", 2);
        Long first = members.addPost(owner.defaultCategoryId(), "하나", "public");
        Long second = members.addPost(daily, "둘", "private");
        Member other = register("f");
        Long othersPost = members.addPost(other.defaultCategoryId(), "남의 글", "public");

        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(count(first) + count(second)).isZero();
        assertThat(listener.heard).containsExactlyInAnyOrder(first, second);
        assertThat(jdbc.queryForObject("select count(*) from category where blog_id = ?", Long.class, owner.blogId())).isZero();
        assertThat(count(othersPost)).isOne();
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private long count(Long postId) {
        return jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, postId);
    }

    /** 글을 지울 때 딸린 것을 지우는 모듈(005) 대신 듣는다. failWith가 있으면 그 예외를 던진다. */
    static class DeletingListener {

        final List<Long> heard = new ArrayList<>();
        final AtomicReference<RuntimeException> failWith = new AtomicReference<>();

        @EventListener
        void on(PostDeletingEvent event) {
            heard.add(event.postId());
            RuntimeException failure = failWith.get();
            if (failure != null) {
                throw failure;
            }
        }
    }

    @TestConfiguration
    static class Listeners {

        @Bean
        DeletingListener deletingListener() {
            return new DeletingListener();
        }
    }
}
