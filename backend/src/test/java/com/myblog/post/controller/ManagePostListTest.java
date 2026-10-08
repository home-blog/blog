package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 블로그 관리의 글 목록 (specs/006 US2, T017, quickstart S-2, FR-012 ~ FR-014, FR-017, SC-002).
 * 비공개 글까지 내 글만, 최신순 10개씩. 거르기 값은 정해진 것만 받는다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class ManagePostListTest {

    private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("o");
        session = members.login(mvc, owner);
    }

    @Test
    void S2_1_글이_없으면_빈_목록이고_hasAnyPost가_거짓() throws Exception {
        mvc.perform(get("/api/manage/posts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.hasAnyPost").value(false));
    }

    @Test
    void S2_2_공개_6_비공개_5면_1쪽_10개_2쪽_1개이고_빠짐도_겹침도_없다() throws Exception {
        List<Long> ids = addMixed(6, 5);
        String first = mvc.perform(get("/api/manage/posts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalCount").value(11))
                .andExpect(jsonPath("$.hasAnyPost").value(true))
                .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(get("/api/manage/posts").param("page", "2").session(session))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        List<Long> seen = new ArrayList<>(postIds(first));
        seen.addAll(postIds(second));
        // 최신순: 마지막에 쓴 글이 맨 위
        assertThat(seen).containsExactlyElementsOf(ids.reversed());
    }

    @Test
    void S2_2_같은_시각이면_번호가_큰_글이_위() throws Exception {
        Instant same = BASE.plus(1, ChronoUnit.DAYS);
        Long older = members.addPost(owner.defaultCategoryId(), "먼저", "public", same);
        Long newer = members.addPost(owner.defaultCategoryId(), "나중", "public", same);
        String body = mvc.perform(get("/api/manage/posts").session(session)).andReturn().getResponse().getContentAsString();
        assertThat(postIds(body)).containsExactly(newer, older);
    }

    @Test
    void S2_3_한_줄에_제목_분류_작성일_공개여부_조회수_댓글수가_있다() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 2);
        Long postId = members.addPost(daily, "첫 글", "private", BASE);
        jdbc.update("update post set views = 7 where post_id = ?", postId);
        Member reader = members.register("r");
        Long commented = members.addPost(owner.defaultCategoryId(), "댓글 달린 글", "public", BASE.plusSeconds(60));
        MockHttpSession readerSession = members.login(mvc, reader);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/posts/{id}/comments", commented).with(csrf()).session(readerSession)
                            .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "댓글" + i)))
                    .andExpect(status().isCreated());
            jdbc.update("update comment set created_at = created_at - interval '1 minute' where users_id = ?", reader.id());
        }

        mvc.perform(get("/api/manage/posts").session(session))
                .andExpect(jsonPath("$.items[0].postId").value(commented))
                .andExpect(jsonPath("$.items[0].commentCount").value(2))
                .andExpect(jsonPath("$.items[1].postId").value(postId))
                .andExpect(jsonPath("$.items[1].title").value("첫 글"))
                .andExpect(jsonPath("$.items[1].category.categoryId").value(daily))
                .andExpect(jsonPath("$.items[1].category.name").value("일상"))
                .andExpect(jsonPath("$.items[1].createdAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[1].visibility").value("private"))
                .andExpect(jsonPath("$.items[1].views").value(7))
                .andExpect(jsonPath("$.items[1].commentCount").value(0));
    }

    @Test
    void S2_4_공개여부로_거르면_11_6_5() throws Exception {
        addMixed(6, 5);
        mvc.perform(get("/api/manage/posts").param("visibility", "all").session(session))
                .andExpect(jsonPath("$.totalCount").value(11));
        mvc.perform(get("/api/manage/posts").param("visibility", "public").session(session))
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.items[*].visibility", everyIs("public")));
        mvc.perform(get("/api/manage/posts").param("visibility", "private").session(session))
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.items[*].visibility", everyIs("private")));
    }

    @Test
    void S2_5_분류와_비공개를_함께_주면_둘_다_맞는_글만이고_없으면_빈_목록에_hasAnyPost는_참() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 2);
        Long match = members.addPost(daily, "일상 비공개", "private", BASE);
        members.addPost(daily, "일상 공개", "public", BASE.plusSeconds(1));
        members.addPost(owner.defaultCategoryId(), "미분류 비공개", "private", BASE.plusSeconds(2));

        String body = mvc.perform(get("/api/manage/posts").param("categoryId", daily.toString())
                        .param("visibility", "private").session(session))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(postIds(body)).containsExactly(match);

        Long empty = members.addCategory(owner.blogId(), "빈 분류", "public", 3);
        mvc.perform(get("/api/manage/posts").param("categoryId", empty.toString()).session(session))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.hasAnyPost").value(true));
    }

    @Test
    void 비공개_분류의_글도_주인에게는_나온다() throws Exception {
        Long secret = members.addCategory(owner.blogId(), "비밀", "private", 2);
        Long postId = members.addPost(secret, "비밀 분류의 공개 글", "public", BASE);
        String body = mvc.perform(get("/api/manage/posts").session(session)).andReturn().getResponse().getContentAsString();
        assertThat(postIds(body)).containsExactly(postId);
    }

    @Test
    void S2_9_정해진_값이_아니면_400이고_내부_정보가_없다() throws Exception {
        for (String[] query : List.of(new String[] {"visibility", "abc"}, new String[] {"page", "-1"},
                new String[] {"page", "0"}, new String[] {"page", "abc"}, new String[] {"categoryId", "' OR 1=1 --"},
                new String[] {"visibility", "' OR 1=1 --"})) {
            String body = mvc.perform(get("/api/manage/posts").param(query[0], query[1]).session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("Exception", "select", "com.myblog");
        }
    }

    @Test
    void 남의_분류_번호로_거르면_없는_분류와_같은_404() throws Exception {
        Member other = members.register("x");
        members.addPost(other.defaultCategoryId(), "남의 글", "public", BASE);
        mvc.perform(get("/api/manage/posts").param("categoryId", other.defaultCategoryId().toString()).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void 회원_B는_A의_글을_하나도_받지_않는다() throws Exception {
        addMixed(2, 2);
        Member b = members.register("b");
        Long bPost = members.addPost(b.defaultCategoryId(), "B의 글", "private", BASE);
        MockHttpSession bSession = members.login(mvc, b);
        String body = mvc.perform(get("/api/manage/posts").session(bSession))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(postIds(body)).containsExactly(bPost);
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mvc.perform(get("/api/manage/posts")).andExpect(status().isUnauthorized());
    }

    /** 공개 글 publicCount개, 이어서 비공개 글 privateCount개를 1분 간격으로 쓴다. 쓴 순서대로 번호를 돌려준다. */
    private List<Long> addMixed(int publicCount, int privateCount) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < publicCount + privateCount; i++) {
            String visibility = i < publicCount ? "public" : "private";
            ids.add(members.addPost(owner.defaultCategoryId(), "글" + i, visibility, BASE.plus(i, ChronoUnit.MINUTES)));
        }
        return ids;
    }

    private static List<Long> postIds(String body) {
        List<Number> ids = JsonPath.read(body, "$.items[*].postId");
        return ids.stream().map(Number::longValue).toList();
    }

    private static org.hamcrest.Matcher<Iterable<? extends String>> everyIs(String value) {
        return org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(value));
    }
}
