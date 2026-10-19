package com.myblog.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * 문장 수집과 필사 노트 (specs/015 US3, S3-1 ~ S3-8). 한도는 시험하기 쉽게 3개로 줄인다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
@TestPropertySource(properties = {"community.clip.max-per-member=3", "community.clip.max-length=40"})
class SentenceClipTest {

    private static final String CLIPS = "/api/v1/me/clips";
    private static final String BODY = """
            # 바다에서

            오늘은 **바다가 유난히** 맑았다.
            파도는 낮고, 바람은 [소금 냄새](https://example.com)를 실어 왔다.

            - 첫째, 일찍 일어날 것
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    private MockMvc mvc;
    private Member writer;
    private Member reader;
    private MockHttpSession writerSession;
    private MockHttpSession readerSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        writer = members.register("clipw");
        reader = members.register("clipr");
        writerSession = members.login(mvc, writer);
        readerSession = members.login(mvc, reader);
        postId = posts.add(writer.defaultCategoryId(), "바다 일기", BODY, "public", Instant.now());
    }

    private ResultActions clip(MockHttpSession session, Long postId, String text) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("postId", postId, "text", text));
        var request = post(CLIPS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(session == null ? request : request.session(session));
    }

    private long clipId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.clipId")).longValue();
    }

    @Test
    void 본문의_문장을_모으면_201이고_필사_노트에_최근_순으로_쌓인다() throws Exception {
        clip(readerSession, postId, "  오늘은 바다가 유난히 맑았다.  ").andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("오늘은 바다가 유난히 맑았다."))
                .andExpect(jsonPath("$.post.postId").value(postId))
                .andExpect(jsonPath("$.post.title").value("바다 일기"))
                .andExpect(jsonPath("$.post.blogName").isNotEmpty())
                .andExpect(jsonPath("$.post.available").value(true));
        clip(readerSession, postId, "파도는 낮고").andExpect(status().isCreated());

        mvc.perform(get(CLIPS).session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].text").value("파도는 낮고"))
                .andExpect(jsonPath("$.items[1].text").value("오늘은 바다가 유난히 맑았다."));
    }

    @Test
    void S3_1_로그인하지_않으면_401이고_볼_수_없는_글은_404() throws Exception {
        clip(null, postId, "파도는 낮고").andExpect(status().isUnauthorized());
        mvc.perform(get(CLIPS)).andExpect(status().isUnauthorized());

        Long hidden = posts.add(writer.defaultCategoryId(), "비공개", "비밀 문장이다", "private", Instant.now());
        clip(readerSession, hidden, "비밀 문장").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        clip(readerSession, 999_999_999L, "파도는 낮고").andExpect(status().isNotFound());
    }

    @Test
    void S3_2_문장은_1자에서_정한_길이까지() throws Exception {
        clip(readerSession, postId, "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIP_TEXT_INVALID"));
        clip(readerSession, postId, "가".repeat(41)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIP_TEXT_INVALID"));
        clip(readerSession, postId, "맑").andExpect(status().isCreated()); // 1자
    }

    @Test
    void S3_3_본문에_있는_문장만_받고_마크다운_기호와_띄어쓰기_차이는_무시한다() throws Exception {
        // 화면에서 고른 글자에는 ** 와 링크 주소가 없고, 줄바꿈이 섞일 수 있다
        clip(readerSession, postId, "바다가 유난히").andExpect(status().isCreated());
        clip(readerSession, postId, "바람은 소금 냄새를 실어 왔다.").andExpect(status().isCreated());
        clip(readerSession, postId, "맑았다.\n파도는").andExpect(jsonPath("$.text").value("맑았다. 파도는"));

        clip(readerSession, postId, "본문에 없는 말").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIP_NOT_IN_POST"));
        clip(readerSession, postId, "example.com").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIP_NOT_IN_POST"));
    }

    @Test
    void 목록_기호와_제목_기호도_무시한다() throws Exception {
        clip(readerSession, postId, "첫째, 일찍").andExpect(status().isCreated());
        clip(readerSession, postId, "바다에서 오늘은").andExpect(status().isCreated());
    }

    @Test
    void S3_4_같은_글의_같은_문장은_다시_만들지_않고_앞의_것을_돌려준다() throws Exception {
        long first = clipId(clip(readerSession, postId, "파도는 낮고").andExpect(status().isCreated()));
        long again = clipId(clip(readerSession, postId, " 파도는  낮고 ").andExpect(status().isOk()));

        assertThat(again).isEqualTo(first);
        assertThat(jdbc.queryForObject("select count(*) from sentence_clip where users_id = ?", Long.class, reader.id()))
                .isOne();
    }

    @Test
    void S3_5_한_사람이_모을_수_있는_문장은_정한_수까지() throws Exception {
        clip(readerSession, postId, "오늘은").andExpect(status().isCreated());
        clip(readerSession, postId, "파도는").andExpect(status().isCreated());
        clip(readerSession, postId, "바람은").andExpect(status().isCreated());

        clip(readerSession, postId, "맑았다").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIP_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("3")));
        // 이미 모은 문장을 다시 보내는 것은 한도와 상관없이 된다
        clip(readerSession, postId, "오늘은").andExpect(status().isOk());
    }

    @Test
    void S3_6_필사_노트는_나만_보고_남의_문장을_지우면_404() throws Exception {
        long mine = clipId(clip(readerSession, postId, "파도는 낮고"));

        mvc.perform(get(CLIPS).session(writerSession)).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(delete(CLIPS + "/{id}", mine).with(csrf()).session(writerSession)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIP_NOT_FOUND"));
        mvc.perform(delete(CLIPS + "/{id}", mine).with(csrf()).session(readerSession)).andExpect(status().isNoContent());
        mvc.perform(delete(CLIPS + "/{id}", mine).with(csrf()).session(readerSession)).andExpect(status().isNotFound());
        mvc.perform(get(CLIPS).session(readerSession)).andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void 원문이_비공개가_되거나_지워져도_문장은_남고_볼_수_없는_글로_보인다() throws Exception {
        clip(readerSession, postId, "파도는 낮고");
        Long other = posts.add(writer.defaultCategoryId(), "지울 글", "곧 사라질 문장", "public", Instant.now());
        clip(readerSession, other, "곧 사라질");

        jdbc.update("update post set visibility = 'private' where post_id = ?", postId);
        mvc.perform(delete("/api/v1/posts/{id}", other).with(csrf()).session(writerSession))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(get(CLIPS).session(readerSession))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].text").value("곧 사라질"))
                .andExpect(jsonPath("$.items[0].post.postId").doesNotExist())
                .andExpect(jsonPath("$.items[0].post.title").value("지울 글"))
                .andExpect(jsonPath("$.items[0].post.available").value(false))
                .andExpect(jsonPath("$.items[1].post.postId").value(postId))
                .andExpect(jsonPath("$.items[1].post.available").value(false));
    }

    @Test
    void S3_7_모은_사람_수는_글_주인만_보고_한_사람은_한_명으로_센다() throws Exception {
        Member third = members.register("clipt");
        MockHttpSession thirdSession = members.login(mvc, third);
        clip(readerSession, postId, "파도는 낮고");
        clip(readerSession, postId, "바람은");
        clip(thirdSession, postId, "오늘은");

        String collectors = "/api/v1/posts/{id}/clips/collectors";
        mvc.perform(get(collectors, postId).session(writerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.collectors").value(2));
        mvc.perform(get(collectors, postId).session(readerSession)).andExpect(status().isNotFound());
        mvc.perform(get(collectors, postId)).andExpect(status().isUnauthorized());
    }

    @Test
    void S3_8_탈퇴하면_그_사람의_필사_노트를_지운다() throws Exception {
        clip(readerSession, postId, "파도는 낮고");
        clip(readerSession, postId, "바람은");

        mvc.perform(post("/api/v1/account/withdrawal").with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from sentence_clip where users_id = ?", Long.class, reader.id()))
                .isZero();
    }
}
