package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Map;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글쓰기 (specs/003 US2, quickstart S-2, S-2a, S-3, S-4, S-12의 6 ~ 8, T017). 실제 PostgreSQL로 확인한다.
 * 동시 요청(S-5)은 PostRequestKeyTest에서 본다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class PostCreateTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;
    private Long travel;
    private Long food;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        session = members.login(mvc, owner);
        travel = jdbc.queryForObject("select topic_id from topic where topic_name = '여행'", Long.class);
        food = jdbc.queryForObject("select topic_id from topic where topic_name = '음식'", Long.class);
    }

    @Test
    void S2_1_처음_여는_글쓰기_화면은_미분류_주제_없음_공개() throws Exception {
        mvc.perform(get("/api/me/blog/post-form").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("미분류"))
                .andExpect(jsonPath("$.defaultCategoryId").value(owner.defaultCategoryId()))
                .andExpect(jsonPath("$.defaultTopicId").isEmpty())
                .andExpect(jsonPath("$.defaultVisibility").value("public"))
                .andExpect(jsonPath("$.topics.length()").value(5))
                .andExpect(jsonPath("$.topics[0].name").value("여행"))
                .andExpect(jsonPath("$.topics[4].name").value("개발"))
                .andExpect(jsonPath("$.limits.titleMaxLength").value(100))
                .andExpect(jsonPath("$.limits.contentMaxLength").value(10000));
    }

    @Test
    void S2_2_3_저장하면_201과_글_번호_작성_시각은_서버가_넣고_수정_시각은_비어_있다() throws Exception {
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Long postId = create(body("첫 글", "마크다운 **원문**", owner.defaultCategoryId(), travel)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(json -> Long.valueOf(JsonPath.read(json, "$.postId").toString()));

        Map<String, Object> row = jdbc.queryForMap("select * from post where post_id = ?", postId);
        assertThat(row.get("title")).isEqualTo("첫 글");
        assertThat(row.get("content")).isEqualTo("마크다운 **원문**");
        assertThat(row.get("visibility")).isEqualTo("public");
        assertThat(row.get("topic_id")).isEqualTo(travel);
        assertThat(row.get("updated_at")).isNull();
        assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant()).isAfterOrEqualTo(before);
    }

    @Test
    void S2_4_S2a_4_다음_글쓰기의_기본값은_마지막에_쓴_글의_분류와_주제() throws Exception {
        Long daily = members.addCategory(owner.blogId(), "일상", "public", 2);
        create(body("하나", "본문", owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        create(body("둘", "본문", daily, food)).andExpect(status().isCreated());

        mvc.perform(get("/api/me/blog/post-form").session(session))
                .andExpect(jsonPath("$.defaultCategoryId").value(daily))
                .andExpect(jsonPath("$.defaultTopicId").value(food))
                .andExpect(jsonPath("$.categories.length()").value(2));
    }

    @Test
    void S2_5_요청의_작성_시각과_블로그_번호와_작성자는_무시한다() throws Exception {
        Member other = members.register("b");
        String json = TestJson.of("title", "어제 글", "content", "본문", "categoryId", owner.defaultCategoryId(),
                "topicId", travel, "createdAt", "2020-01-01T00:00:00Z", "blogId", other.blogId(), "userId", other.id());
        Long postId = Long.valueOf(JsonPath.read(create(json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.postId").toString());

        Instant createdAt = jdbc.queryForObject("select created_at from post where post_id = ?", java.sql.Timestamp.class, postId)
                .toInstant();
        assertThat(createdAt).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(postCountOf(other)).isZero();
    }

    @Test
    void 공개_여부를_비공개로_고를_수_있고_다른_값은_거절한다() throws Exception {
        create(TestJson.of("title", "비밀", "content", "본문", "categoryId", owner.defaultCategoryId(), "topicId", travel,
                "visibility", "private")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select p.visibility from post p join category c using (category_id) where c.blog_id = ?",
                String.class, owner.blogId())).isEqualTo("private");

        expectFieldError(TestJson.of("title", "x", "content", "본문", "categoryId", owner.defaultCategoryId(), "topicId", travel,
                "visibility", "friends"), "visibility", "※ 공개 여부를 다시 골라 주세요");
    }

    @Test
    void S2a_2_3_주제를_고르지_않거나_없는_주제면_주제를_골라_주세요() throws Exception {
        expectFieldError(body("제목", "본문", owner.defaultCategoryId(), null), "topicId", "주제를 골라 주세요");
        expectFieldError(body("제목", "본문", owner.defaultCategoryId(), 999999L), "topicId", "주제를 골라 주세요");
        assertThat(postCountOf(owner)).isZero();
    }

    @Test
    void S2a_7_주제가_비어_있는_글은_없다() throws Exception {
        create(body("제목", "본문", owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from post where topic_id is null", Long.class)).isZero();
    }

    @Test
    void S3_1_2_제목이_비었거나_공백만이면_제목을_입력해_주세요() throws Exception {
        expectFieldError(body("", "본문", owner.defaultCategoryId(), travel), "title", "제목을 입력해 주세요");
        expectFieldError(body("   ", "본문", owner.defaultCategoryId(), travel), "title", "제목을 입력해 주세요");
        expectFieldError(TestJson.of("content", "본문", "categoryId", owner.defaultCategoryId(), "topicId", travel),
                "title", "제목을 입력해 주세요");
        assertThat(postCountOf(owner)).isZero();
    }

    @Test
    void S3_3_9_본문이_비었거나_공백과_줄바꿈만이면_본문을_입력해_주세요() throws Exception {
        expectFieldError(body("제목", "", owner.defaultCategoryId(), travel), "content", "본문을 입력해 주세요");
        expectFieldError(body("제목", "  \n\r\n\t ", owner.defaultCategoryId(), travel), "content", "본문을 입력해 주세요");
    }

    @Test
    void S3_4_제목과_본문이_모두_비면_두_칸을_함께() throws Exception {
        create(body("", "", owner.defaultCategoryId(), travel))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')].message").value("제목을 입력해 주세요"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'content')].message").value("본문을 입력해 주세요"));
    }

    @Test
    void S3_5_6_8_제목은_100자_본문은_원문_10000자까지_이모지는_한_글자() throws Exception {
        create(body("가".repeat(100), "나".repeat(10000), owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        create(body("😀".repeat(50) + "가".repeat(50), "본문", owner.defaultCategoryId(), travel)).andExpect(status().isCreated());

        expectFieldError(body("가".repeat(101), "본문", owner.defaultCategoryId(), travel), "title", "※ 제목은 100자 이하로 입력해 주세요");
        expectFieldError(body("제목", "나".repeat(10001), owner.defaultCategoryId(), travel), "content",
                "※ 본문은 10,000자 이하로 입력해 주세요");
    }

    @Test
    void S3_D2_본문은_마크다운_기호와_이미지_주소까지_원문으로_센다() throws Exception {
        String image = "![사진](https://example.com/a.png)"; // 31자
        String exact = image + "가".repeat(10000 - image.length());
        create(body("제목", exact, owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        expectFieldError(body("제목", exact + "**", owner.defaultCategoryId(), travel), "content",
                "※ 본문은 10,000자 이하로 입력해 주세요");
        // 줄바꿈 \r\n은 \n 한 글자로 맞춘 뒤 센다
        create(body("제목", "가".repeat(4999) + "\r\n".repeat(5001), owner.defaultCategoryId(), travel))
                .andExpect(status().isCreated());
    }

    @Test
    void S3_7_제목_앞뒤_공백은_지우고_본문은_줄바꿈만_맞춘다() throws Exception {
        create(body("  앞뒤 공백  ", "  첫 줄\r\n둘째 줄  ", owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        Map<String, Object> row = jdbc.queryForMap(
                "select title, content from post p join category c using (category_id) where c.blog_id = ?", owner.blogId());
        assertThat(row.get("title")).isEqualTo("앞뒤 공백");
        assertThat(row.get("content")).isEqualTo("  첫 줄\n둘째 줄  ");
    }

    @Test
    void 분류를_고르지_않으면_분류를_골라_주세요() throws Exception {
        expectFieldError(TestJson.of("title", "제목", "content", "본문", "topicId", travel), "categoryId", "※ 분류를 골라 주세요");
    }

    @Test
    void S4_1_로그인하지_않으면_401() throws Exception {
        mvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("제목", "본문", owner.defaultCategoryId(), travel)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/me/blog/post-form")).andExpect(status().isUnauthorized());
    }

    @Test
    void S4_3_남의_분류_번호면_INVALID_CATEGORY이고_그_블로그에_글이_생기지_않는다() throws Exception {
        Member other = members.register("b");
        create(body("제목", "본문", other.defaultCategoryId(), travel))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"));
        create(body("제목", "본문", 999999999L, travel)).andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));
        assertThat(postCountOf(other)).isZero();
    }

    @Test
    void S12_6_SQL_같은_글자는_그대로_저장된다() throws Exception {
        String title = "'; DROP TABLE post; --";
        create(body(title, "' OR 1=1 --", owner.defaultCategoryId(), travel)).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select title from post p join category c using (category_id) where c.blog_id = ?",
                String.class, owner.blogId())).isEqualTo(title);
    }

    @Test
    void S12_7_CSRF_토큰이_없으면_거절한다() throws Exception {
        mvc.perform(post("/api/posts").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body("제목", "본문", owner.defaultCategoryId(), travel)))
                .andExpect(status().isForbidden());
        assertThat(postCountOf(owner)).isZero();
    }

    @Test
    void S12_9_이상한_JSON과_숫자_자리의_글자는_400이고_내부_정보가_없다() throws Exception {
        create("{\"title\": ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("입력값을 다시 확인해 주세요"));
        create("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":\"abc\",\"topicId\":1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/posts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void expectFieldError(String json, String field, String message) throws Exception {
        create(json).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(message));
    }

    private static String body(String title, String content, Long categoryId, Long topicId) {
        return TestJson.of("title", title, "content", content, "categoryId", categoryId, "topicId", topicId);
    }

    private long postCountOf(Member member) {
        return jdbc.queryForObject("select count(*) from post p join category c using (category_id) where c.blog_id = ?",
                Long.class, member.blogId());
    }
}
