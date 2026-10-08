package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 읽기 (specs/003 US3, quickstart S-6, S-9의 5·6, S-9a의 4, T026). 실제 PostgreSQL로 확인한다.
 * 볼 수 없는 글은 없는 글과 상태 코드·본문이 글자 단위로 같아야 한다 (FR-026, SC-003).
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class PostReadTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession ownerSession;
    private MockHttpSession otherSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        ownerSession = members.login(mvc, owner);
        otherSession = members.login(mvc, members.register("b"));
    }

    @Test
    void S6_1_로그인하지_않고_공개_글을_읽는다_수정_시각은_비어_있고_줄바꿈은_그대로() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "첫 글", "public", T0);
        jdbc.update("update post set content = ? where post_id = ?", "첫 줄\n\n**둘째** 줄", postId);

        mvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.blogName").value(owner.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.category.categoryId").value(owner.defaultCategoryId()))
                .andExpect(jsonPath("$.category.name").value("미분류"))
                .andExpect(jsonPath("$.category.visibility").value("public"))
                .andExpect(jsonPath("$.topic.name").value("여행"))
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.content").value("첫 줄\n\n**둘째** 줄"))
                .andExpect(jsonPath("$.visibility").value("public"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").isEmpty())
                .andExpect(jsonPath("$.prevPostId").isEmpty())
                .andExpect(jsonPath("$.nextPostId").isEmpty())
                .andExpect(jsonPath("$.isOwner").value(false));
    }

    @Test
    void S6_2_3_4_이전_다음은_바로_앞뒤의_공개_글이고_사이의_비공개_글은_건너뛴다() throws Exception {
        Long first = members.addPost(owner.defaultCategoryId(), "1", "public", T0);
        Long hidden = members.addPost(owner.defaultCategoryId(), "비공개", "private", T0.plus(1, ChronoUnit.HOURS));
        Long second = members.addPost(owner.defaultCategoryId(), "2", "public", T0.plus(2, ChronoUnit.HOURS));
        Long secretCategory = members.addCategory(owner.blogId(), "비밀", "private", 2);
        members.addPost(secretCategory, "비공개 분류의 공개 글", "public", T0.plus(3, ChronoUnit.HOURS));
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 3);
        Long third = members.addPost(daily, "3", "public", T0.plus(4, ChronoUnit.HOURS));

        expectNeighbors(second, null, first, third);
        expectNeighbors(first, null, null, second);
        expectNeighbors(third, null, second, null);
        // 주인이 자기 비공개 글을 봐도 이전·다음은 공개 글만 가리킨다
        expectNeighbors(hidden, ownerSession, first, second);
    }

    @Test
    void 작성_시각이_같으면_글_번호_순서다() throws Exception {
        Long first = members.addPost(owner.defaultCategoryId(), "1", "public", T0);
        Long second = members.addPost(owner.defaultCategoryId(), "2", "public", T0);
        expectNeighbors(first, null, null, second);
        expectNeighbors(second, null, first, null);
    }

    @Test
    void 다른_블로그의_글은_이전_다음에_나오지_않는다() throws Exception {
        Long mine = members.addPost(owner.defaultCategoryId(), "내 글", "public", T0);
        Member other = members.register("c");
        members.addPost(other.defaultCategoryId(), "남의 글", "public", T0.plus(1, ChronoUnit.HOURS));
        expectNeighbors(mine, null, null, null);
    }

    @Test
    void S6_6_없는_번호는_404_존재하지_않는_글입니다() throws Exception {
        mvc.perform(get("/api/posts/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 글입니다"));
    }

    @Test
    void S6_7_주인만_isOwner가_참이다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public", T0);
        mvc.perform(get("/api/posts/{id}", postId).session(ownerSession)).andExpect(jsonPath("$.isOwner").value(true));
        mvc.perform(get("/api/posts/{id}", postId).session(otherSession)).andExpect(jsonPath("$.isOwner").value(false));
    }

    @Test
    void S9_2_주인은_자기_비공개_글과_비공개_분류의_글을_읽는다() throws Exception {
        Long privatePost = members.addPost(owner.defaultCategoryId(), "비공개", "private", T0);
        Long secret = members.addCategory(owner.blogId(), "비밀", "private", 2);
        Long inSecret = members.addPost(secret, "비공개 분류의 글", "public", T0);
        mvc.perform(get("/api/posts/{id}", privatePost).session(ownerSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visibility").value("private"));
        mvc.perform(get("/api/posts/{id}", inSecret).session(ownerSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category.visibility").value("private"));
    }

    @Test
    void S9_5_6_S9a_4_남의_비공개_글과_비공개_분류의_글은_없는_번호와_글자_단위로_같은_404() throws Exception {
        Long privatePost = members.addPost(owner.defaultCategoryId(), "비공개", "private", T0);
        Long secret = members.addCategory(owner.blogId(), "비밀", "private", 2);
        Long inSecret = members.addPost(secret, "비공개 분류의 공개 글", "public", T0);
        MockHttpServletResponse missing = mvc.perform(get("/api/posts/999999999")).andReturn().getResponse();

        for (Long postId : new Long[] {privatePost, inSecret}) {
            for (MockHttpSession session : new MockHttpSession[] {new MockHttpSession(), otherSession}) {
                MockHttpServletResponse response = mvc.perform(get("/api/posts/{id}", postId).session(session))
                        .andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(missing.getStatus()).isEqualTo(404);
                assertThat(response.getContentAsString()).isEqualTo(missing.getContentAsString());
            }
        }
    }

    @Test
    void 블로그와_분류_이름은_요청마다_표에서_읽는다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public", T0);
        jdbc.update("update blog set name = '새 이름' where blog_id = ?", owner.blogId());
        jdbc.update("update category set name = '바뀐 분류' where category_id = ?", owner.defaultCategoryId());
        mvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.blogName").value("새 이름"))
                .andExpect(jsonPath("$.category.name").value("바뀐 분류"));
    }

    private void expectNeighbors(Long postId, MockHttpSession session, Long prev, Long next) throws Exception {
        mvc.perform(get("/api/posts/{id}", postId).session(session == null ? new MockHttpSession() : session))
                .andExpect(status().isOk())
                .andExpect(prev == null ? jsonPath("$.prevPostId").isEmpty() : jsonPath("$.prevPostId").value(prev))
                .andExpect(next == null ? jsonPath("$.nextPostId").isEmpty() : jsonPath("$.nextPostId").value(next));
    }
}
