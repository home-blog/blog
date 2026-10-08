package com.myblog.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestStats;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
 * 블로그를 닫거나 글을 지울 때 통계도 지운다 (specs/006 T010, T011). 통계 줄이 있어도 탈퇴·글 삭제가 성공해야 한다.
 * 클래스 전체에 @Transactional을 걸지 않는다 (실제 탈퇴·삭제 트랜잭션을 그대로 본다). 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestStats.class})
class StatsCleanerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private TestStats stats;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private Member other;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        other = register("x");
    }

    @AfterEach
    void cleanUp() {
        for (Member member : created) {
            stats.deleteFor(member.blogId());
            jdbc.update("delete from spring_session where principal_name = ?", String.valueOf(member.id()));
            testComments.deleteMember(member);
        }
    }

    @Test
    void 통계_줄이_있는_회원이_탈퇴하면_성공하고_그_블로그와_글의_통계가_남지_않는다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        stats.addBlogDay(owner.blogId(), DAY, 3, 2);
        stats.addBlogDay(owner.blogId(), DAY.plusDays(1), 1, 1);
        stats.addPostDay(postId, DAY, 3);
        Long othersPost = members.addPost(other.defaultCategoryId(), "남의 글", "public");
        stats.addBlogDay(other.blogId(), DAY, 5, 4);
        stats.addPostDay(othersPost, DAY, 5);
        MockHttpSession session = members.login(mvc, owner);

        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(stats.blogRows(owner.blogId())).isZero();
        assertThat(stats.postRows(postId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from blog where blog_id = ?", Long.class, owner.blogId())).isZero();
        // 다른 블로그의 줄은 그대로
        assertThat(stats.blogRows(other.blogId())).isOne();
        assertThat(stats.postRows(othersPost)).isOne();
    }

    @Test
    void 글별_통계_줄이_있는_글을_지우면_204이고_그_줄만_사라진다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "지울 글", "public");
        Long kept = members.addPost(owner.defaultCategoryId(), "남길 글", "public");
        stats.addPostDay(postId, DAY, 2);
        stats.addPostDay(postId, DAY.plusDays(1), 1);
        stats.addPostDay(kept, DAY, 7);
        stats.addBlogDay(owner.blogId(), DAY, 9, 3);
        MockHttpSession session = members.login(mvc, owner);

        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(session)).andExpect(status().isNoContent());

        assertThat(stats.postRows(postId)).isZero();
        assertThat(stats.postRows(kept)).isOne();
        // 지운 글의 조회수도 블로그 일별 통계에는 남는다 (누적은 그 합, D-7)
        assertThat(stats.blogRows(owner.blogId())).isOne();
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
