package com.myblog.post.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 연재 (specs/013 US2, S2-1 ~ S2-7, contracts 2). 화 번호는 보는 사람에게 보이는 글로 센다 (D-10).
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class SeriesApiTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        session = members.login(mvc, owner);
    }

    private ResultActions create(MockHttpSession who, String name, String description) throws Exception {
        return mvc.perform(post("/api/v1/me/blog/series").session(who).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", name, "description", description)));
    }

    private Long createSeries(String name) throws Exception {
        String body = create(session, name, "").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.seriesId")).longValue();
    }

    private ResultActions addPost(MockHttpSession who, Long seriesId, Long postId) throws Exception {
        return mvc.perform(put("/api/v1/me/blog/series/{s}/posts/{p}", seriesId, postId).session(who).with(csrf()));
    }

    private Long newPost(String title, String visibility, int minutes) {
        return posts.add(owner.defaultCategoryId(), title, "본문", visibility, BASE.plusSeconds(minutes * 60L));
    }

    @Nested
    @DisplayName("S2-1 이름·소개 경계값")
    class NameRules {

        @Test
        void 이름_1자와_30자는_된다() throws Exception {
            create(session, "가", "").andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("가"));
            create(session, "나".repeat(30), "").andExpect(status().isCreated());
        }

        @Test
        void 이모지는_한_글자로_센다() throws Exception {
            create(session, "😀".repeat(30), "").andExpect(status().isCreated());
        }

        @Test
        void 이름이_비었거나_공백만이면_거절한다() throws Exception {
            create(session, "", "").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("SERIES_NAME_REQUIRED"));
            create(session, "   ", "").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("SERIES_NAME_REQUIRED"));
        }

        @Test
        void 이름_31자는_거절한다() throws Exception {
            create(session, "다".repeat(31), "").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("SERIES_NAME_TOO_LONG"));
        }

        @Test
        void 소개_200자는_되고_201자는_거절한다() throws Exception {
            create(session, "연재", "라".repeat(200)).andExpect(status().isCreated());
            create(session, "연재", "라".repeat(201)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("SERIES_DESCRIPTION_TOO_LONG"));
        }

        @Test
        void 고치기도_같은_규칙이다() throws Exception {
            Long seriesId = createSeries("처음");
            mvc.perform(put("/api/v1/me/blog/series/{s}", seriesId).session(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", " 바꾼 이름 ", "description", "소개")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("바꾼 이름"))
                    .andExpect(jsonPath("$.description").value("소개"));
            mvc.perform(put("/api/v1/me/blog/series/{s}", seriesId).session(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "", "description", "")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void S2_2_블로그마다_50개까지다() throws Exception {
        for (int i = 0; i < 49; i++) {
            jdbc.update("insert into series (blog_id, name, created_at) values (?, ?, now())", owner.blogId(), "연재" + i);
        }
        create(session, "50번째", "").andExpect(status().isCreated());
        create(session, "51번째", "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERIES_LIMIT_EXCEEDED"));
    }

    @Test
    void 로그인하지_않으면_401이다() throws Exception {
        create(new MockHttpSession(), "연재", "").andExpect(status().isUnauthorized());
    }

    @Test
    void S2_3_다른_연재로_옮기면_앞_연재에서_빠지고_끝_화가_된다() throws Exception {
        Long a = createSeries("A");
        Long b = createSeries("B");
        Long first = newPost("1", "public", 1);
        Long moving = newPost("2", "public", 2);
        addPost(session, b, first).andExpect(status().isNoContent());
        addPost(session, a, moving).andExpect(status().isNoContent());
        addPost(session, b, moving).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/series/{s}", a)).andExpect(jsonPath("$.posts", hasSize(0)));
        mvc.perform(get("/api/v1/series/{s}", b))
                .andExpect(jsonPath("$.posts[*].postId", contains(first.intValue(), moving.intValue())))
                .andExpect(jsonPath("$.posts[1].episode").value(2));
    }

    @Test
    void 같은_연재에_다시_넣으면_그대로다() throws Exception {
        Long s = createSeries("A");
        Long p1 = newPost("1", "public", 1);
        Long p2 = newPost("2", "public", 2);
        addPost(session, s, p1);
        addPost(session, s, p2);
        addPost(session, s, p1).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/series/{s}", s)).andExpect(jsonPath("$.posts[*].postId", contains(p1.intValue(), p2.intValue())));
    }

    @Nested
    @DisplayName("S2-4 보이는 글로 화 번호")
    class Episodes {

        private Long seriesId;
        private Long first;
        private Long hidden;
        private Long third;

        @BeforeEach
        void setUp() throws Exception {
            seriesId = createSeries("제주 한 달 살기");
            first = newPost("첫날", "public", 1);
            hidden = newPost("둘째 날", "private", 2);
            third = newPost("셋째 날", "public", 3);
            addPost(session, seriesId, first);
            addPost(session, seriesId, hidden);
            addPost(session, seriesId, third);
        }

        @Test
        void 방문자는_비공개_글을_건너뛰고_이어진_번호로_본다() throws Exception {
            mvc.perform(get("/api/v1/series/{s}", seriesId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[*].postId", contains(first.intValue(), third.intValue())))
                    .andExpect(jsonPath("$.posts[1].episode").value(2))
                    .andExpect(jsonPath("$.posts[0].visibility").doesNotExist());
            mvc.perform(get("/api/v1/posts/{p}/series", first))
                    .andExpect(jsonPath("$.series.episode").value(1))
                    .andExpect(jsonPath("$.series.total").value(2))
                    .andExpect(jsonPath("$.series.previous").value(nullValue()))
                    .andExpect(jsonPath("$.series.next.postId").value(third))
                    .andExpect(jsonPath("$.series.next.title").value("셋째 날"));
            mvc.perform(get("/api/v1/posts/{p}/series", third))
                    .andExpect(jsonPath("$.series.episode").value(2))
                    .andExpect(jsonPath("$.series.previous.postId").value(first))
                    .andExpect(jsonPath("$.series.next").value(nullValue()));
        }

        @Test
        void 주인은_모든_글을_본다() throws Exception {
            mvc.perform(get("/api/v1/series/{s}", seriesId).session(session))
                    .andExpect(jsonPath("$.posts", hasSize(3)))
                    .andExpect(jsonPath("$.posts[1].visibility").value("private"))
                    .andExpect(jsonPath("$.posts[2].episode").value(3));
            mvc.perform(get("/api/v1/posts/{p}/series", hidden).session(session))
                    .andExpect(jsonPath("$.series.episode").value(2))
                    .andExpect(jsonPath("$.series.total").value(3));
        }

        @Test
        void 볼_수_없는_글의_연재_정보는_404다() throws Exception {
            mvc.perform(get("/api/v1/posts/{p}/series", hidden)).andExpect(status().isNotFound());
        }

        @Test
        void 블로그_연재_목록의_글_수도_보이는_글로_센다() throws Exception {
            mvc.perform(get("/api/v1/blogs/{b}/series", owner.blogId()))
                    .andExpect(jsonPath("$.items[0].name").value("제주 한 달 살기"))
                    .andExpect(jsonPath("$.items[0].postCount").value(2));
            mvc.perform(get("/api/v1/blogs/{b}/series", owner.blogId()).session(session))
                    .andExpect(jsonPath("$.items[0].postCount").value(3));
        }

        @Test
        void 비공개_분류의_글도_방문자에게는_세지_않는다() throws Exception {
            Long hiddenCategory = members.addCategory(owner.blogId(), "숨김", "private", 2);
            Long inHidden = posts.add(hiddenCategory, "숨긴 분류", "본문", "public", BASE.plusSeconds(600));
            addPost(session, seriesId, inHidden);
            mvc.perform(get("/api/v1/series/{s}", seriesId)).andExpect(jsonPath("$.posts", hasSize(2)));
        }
    }

    @Test
    void 글쓰기_화면에_내_연재_목록이_온다() throws Exception {
        Long s = createSeries("A");
        mvc.perform(get("/api/v1/me/blog/post-form").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].seriesId").value(s))
                .andExpect(jsonPath("$.series[0].name").value("A"))
                .andExpect(jsonPath("$.writing.draftAutosaveSeconds").value(30))
                .andExpect(jsonPath("$.writing.draftMaxCount").value(50))
                .andExpect(jsonPath("$.writing.scheduleMaxDays").value(30));
    }

    @Test
    void 연재에_없는_글은_series가_null이다() throws Exception {
        Long p = newPost("혼자", "public", 1);
        mvc.perform(get("/api/v1/posts/{p}/series", p)).andExpect(status().isOk()).andExpect(jsonPath("$.series").value(nullValue()));
    }

    @Test
    void 없는_연재는_404다() throws Exception {
        mvc.perform(get("/api/v1/series/{s}", 999_999)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERIES_NOT_FOUND"));
    }

    @Nested
    @DisplayName("S2-5 남의 것은 404")
    class Others {

        @Test
        void 남의_연재에_내_글을_넣을_수_없다() throws Exception {
            Member other = members.register("b");
            MockHttpSession otherSession = members.login(mvc, other);
            String body = create(otherSession, "남의 연재", "").andReturn().getResponse().getContentAsString();
            Long othersSeries = ((Number) JsonPath.read(body, "$.seriesId")).longValue();
            addPost(session, othersSeries, newPost("내 글", "public", 1)).andExpect(status().isNotFound());
        }

        @Test
        void 내_연재에_남의_글을_넣을_수_없다() throws Exception {
            Member other = members.register("b");
            Long othersPost = posts.add(other.defaultCategoryId(), "남의 글", "본문", "public", BASE);
            addPost(session, createSeries("내 연재"), othersPost).andExpect(status().isNotFound());
        }

        @Test
        void 남의_연재를_고치거나_지울_수_없다() throws Exception {
            Long mine = createSeries("내 연재");
            MockHttpSession otherSession = members.login(mvc, members.register("b"));
            mvc.perform(put("/api/v1/me/blog/series/{s}", mine).session(otherSession).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "뺏기", "description", "")))
                    .andExpect(status().isNotFound());
            mvc.perform(delete("/api/v1/me/blog/series/{s}", mine).session(otherSession).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("S2-6 연재·글 지우기")
    class Deleting {

        @Test
        void 연재를_지우면_글은_남는다() throws Exception {
            Long s = createSeries("A");
            Long p = newPost("글", "public", 1);
            addPost(session, s, p);
            mvc.perform(delete("/api/v1/me/blog/series/{s}", s).session(session).with(csrf())).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/posts/{p}", p)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/posts/{p}/series", p)).andExpect(jsonPath("$.series").value(nullValue()));
        }

        @Test
        void 글을_지우면_연재에서도_빠진다() throws Exception {
            Long s = createSeries("A");
            Long p = newPost("글", "public", 1);
            addPost(session, s, p);
            mvc.perform(delete("/api/v1/posts/{p}", p).session(session).with(csrf())).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/series/{s}", s)).andExpect(jsonPath("$.posts", hasSize(0)));
        }

        @Test
        void 연재에서_글을_뺄_수_있고_없는_글을_빼면_404다() throws Exception {
            Long s = createSeries("A");
            Long p = newPost("글", "public", 1);
            addPost(session, s, p);
            mvc.perform(delete("/api/v1/me/blog/series/{s}/posts/{p}", s, p).session(session).with(csrf()))
                    .andExpect(status().isNoContent());
            mvc.perform(delete("/api/v1/me/blog/series/{s}/posts/{p}", s, p).session(session).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("S2-7 순서 바꾸기")
    class Ordering {

        private Long s;
        private Long p1;
        private Long p2;
        private Long p3;

        @BeforeEach
        void setUp() throws Exception {
            s = createSeries("A");
            p1 = newPost("1", "public", 1);
            p2 = newPost("2", "public", 2);
            p3 = newPost("3", "public", 3);
            addPost(session, s, p1);
            addPost(session, s, p2);
            addPost(session, s, p3);
        }

        private ResultActions order(String postIds) throws Exception {
            return mvc.perform(put("/api/v1/me/blog/series/{s}/order", s).session(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"postIds\":" + postIds + "}"));
        }

        @Test
        void 모두_한_번씩_보내면_그_순서가_된다() throws Exception {
            order("[" + p3 + "," + p1 + "," + p2 + "]").andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[*].postId", contains(p3.intValue(), p1.intValue(), p2.intValue())));
            mvc.perform(get("/api/v1/posts/{p}/series", p3)).andExpect(jsonPath("$.series.episode").value(1));
        }

        @Test
        void 빠지거나_겹치거나_남의_번호가_있으면_400이다() throws Exception {
            order("[" + p1 + "," + p2 + "]").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SERIES_ORDER_MISMATCH"));
            order("[" + p1 + "," + p1 + "," + p2 + "]").andExpect(status().isBadRequest());
            order("[" + p1 + "," + p2 + "," + p3 + ",999999]").andExpect(status().isBadRequest());
            order("[]").andExpect(status().isBadRequest());
        }
    }

    @Test
    void 연재_하나에_글은_200개까지다() throws Exception {
        Long s = createSeries("긴 연재");
        for (int i = 0; i < 200; i++) {
            Long p = newPost("글" + i, "public", i);
            jdbc.update("insert into series_post (series_id, post_id, sort_order) values (?, ?, ?)", s, p, i);
        }
        addPost(session, s, newPost("201번째", "public", 500)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERIES_POSTS_LIMIT_EXCEEDED"));
    }
}
