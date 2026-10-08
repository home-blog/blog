package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
import com.myblog.support.TestComments;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 태그 (specs/005 US5, T040, quickstart S-9, S-11의 태그 줄, FR-013 ~ FR-016, SC-004, SC-008).
 * 태그 이름은 테스트마다 다르게 만들어 다른 테스트의 글과 섞이지 않게 한다. 태그 줄은 지우지 않아도 된다(글이 지워져도 남는 것이 규칙).
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class PostTagTest {

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
    private MockHttpSession ownerSession;
    private Long topicId;
    /** 이 테스트만의 태그 앞부분 (소문자 6자) */
    private String t;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        ownerSession = members.login(mvc, owner);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        t = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S9_1_샵을_빼고_소문자로_저장하고_글_상세_아래에_보인다() throws Exception {
        Long postId = create(owner.defaultCategoryId(), "public", List.of("#여행" + t, " Java" + t + " ", "##ab" + t));
        mvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.tags", contains("ab" + t, "java" + t, "여행" + t)));
        mvc.perform(get("/api/posts/{id}/edit", postId).session(ownerSession))
                .andExpect(jsonPath("$.tags", contains("ab" + t, "java" + t, "여행" + t)));
    }

    @Test
    void S9_2_태그_없이도_저장한다() throws Exception {
        Long postId = create(owner.defaultCategoryId(), "public", List.of());
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.tags", empty()));
        Long noField = createWithBody(body(owner.defaultCategoryId(), "public", null));
        mvc.perform(get("/api/posts/{id}", noField)).andExpect(jsonPath("$.tags", empty()));
    }

    @Test
    void S9_3_6개면_TAG_TOO_MANY이고_글도_저장하지_않는다() throws Exception {
        long before = postCount();
        write(body(owner.defaultCategoryId(), "public", List.of("a1", "a2", "a3", "a4", "a5", "a6")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tags"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TAG_TOO_MANY"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 태그는 5개까지 붙일 수 있습니다"));
        assertThat(postCount()).isEqualTo(before);
    }

    @Test
    void S9_4_5_16자_공백_쉼표는_TAG_INVALID이고_15자와_샵_더하기_15자는_된다() throws Exception {
        write(body(owner.defaultCategoryId(), "public", List.of("가".repeat(16), "a b", "a,b", "#", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(5))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tags[0]"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TAG_INVALID"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 태그는 공백과 쉼표 없이 1~15자로 입력해 주세요"))
                .andExpect(jsonPath("$.fieldErrors[2].field").value("tags[2]"));
        String fifteen = t + "가".repeat(9);
        Long postId = create(owner.defaultCategoryId(), "public", List.of(fifteen, "#" + t + "나".repeat(9)));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void S9_6_대소문자만_다른_태그는_TAG_DUPLICATED() throws Exception {
        write(body(owner.defaultCategoryId(), "public", List.of("Java" + t, "java" + t)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tags[1]"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TAG_DUPLICATED"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 이미 붙인 태그입니다"));
    }

    @Test
    void S9_7_8_태그_목록은_공개_분류의_공개_글만_최신순이고_주인이_봐도_비공개는_없다() throws Exception {
        String tag = "여행" + t;
        Long first = create(owner.defaultCategoryId(), "public", List.of(tag));
        Long hidden = create(owner.defaultCategoryId(), "private", List.of(tag));
        Long privateCategory = members.addCategory(owner.blogId(), "숨김", "private", 2);
        create(privateCategory, "public", List.of(tag));
        Member other = register("p");
        MockHttpSession otherSession = members.login(mvc, other);
        Long second = createAs(otherSession, other.defaultCategoryId(), "public", List.of("#" + tag.toUpperCase()));

        mvc.perform(get("/api/tags/{name}/posts", tag).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value(tag))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.posts[0].postId").value(second))
                .andExpect(jsonPath("$.posts[1].postId").value(first))
                .andExpect(jsonPath("$.posts[1].blogName").isString())
                .andExpect(jsonPath("$.posts[1].categoryName").value("미분류"))
                .andExpect(jsonPath("$.posts[1].preview").value("본문"));
        // 주소의 이름도 같은 방법으로 다듬는다 (#, 대소문자)
        mvc.perform(get("/api/tags/{name}/posts", "#" + tag.toUpperCase())).andExpect(jsonPath("$.totalCount").value(2));

        // 공개 글을 비공개로 바꾸면 바로 빠진다
        update(first, "private", List.of(tag)).andExpect(status().isOk());
        mvc.perform(get("/api/tags/{name}/posts", tag))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.posts[0].postId").value(second));
        assertThat(hidden).isNotNull();
    }

    @Test
    void 없는_태그나_규칙에_맞지_않는_이름은_빈_목록_200() throws Exception {
        mvc.perform(get("/api/tags/{name}/posts", "없는" + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.posts", empty()));
        mvc.perform(get("/api/tags/{name}/posts", "가".repeat(30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void S9_9_수정에서_보낸_목록이_새_전체_목록이고_태그만_바꿔도_수정됨() throws Exception {
        Long postId = create(owner.defaultCategoryId(), "public", List.of("하나" + t, "둘" + t));
        update(postId, "public", List.of("둘" + t, "하나" + t))
                .andExpect(jsonPath("$.changed").value(false))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
        update(postId, "public", List.of("둘" + t, "셋" + t))
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.updatedAt").isString());
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.tags", contains("둘" + t, "셋" + t)));
        update(postId, "public", null).andExpect(jsonPath("$.changed").value(true));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.tags", empty()));
        // 수정에서도 같은 규칙
        update(postId, "public", List.of("a1", "a2", "a3", "a4", "a5", "a6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TAG_TOO_MANY"));
    }

    @Test
    void S9_12_DB에는_한_글에_5개를_넘거나_겹친_연결이_없다() throws Exception {
        create(owner.defaultCategoryId(), "public", List.of("a" + t, "b" + t, "c" + t, "d" + t, "e" + t));
        assertThat(jdbc.queryForObject("select count(*) from (select post_id from post_tag group by post_id"
                + " having count(*) > 5) x", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from (select lower(name) from tag group by lower(name)"
                + " having count(*) > 1) x", Long.class)).isZero();
    }

    @Test
    void S11_글을_지우면_연결은_지워지고_태그_줄은_남는다() throws Exception {
        Long postId = create(owner.defaultCategoryId(), "public", List.of("남는" + t));
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from post_tag where post_id = ?", Long.class, postId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from tag where name = ?", Long.class, "남는" + t)).isOne();
    }

    private Long create(Long categoryId, String visibility, List<String> tags) throws Exception {
        return createAs(ownerSession, categoryId, visibility, tags);
    }

    private Long createAs(MockHttpSession session, Long categoryId, String visibility, List<String> tags) throws Exception {
        String response = mvc.perform(post("/api/posts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body(categoryId, visibility, tags)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.postId")).longValue();
    }

    private Long createWithBody(String body) throws Exception {
        String response = write(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.postId")).longValue();
    }

    private ResultActions write(String body) throws Exception {
        return mvc.perform(post("/api/posts").with(csrf()).session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(Long postId, String visibility, List<String> tags) throws Exception {
        return mvc.perform(put("/api/posts/{id}", postId).with(csrf()).session(ownerSession)
                .contentType(MediaType.APPLICATION_JSON).content(body(owner.defaultCategoryId(), visibility, tags)));
    }

    private String body(Long categoryId, String visibility, List<String> tags) {
        String tagJson = tags == null ? "" : ",\"tags\":" + tags.stream()
                .map(tag -> "\"" + tag.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"title\":\"제목\",\"content\":\"본문\",\"categoryId\":" + categoryId + ",\"topicId\":" + topicId
                + ",\"visibility\":\"" + visibility + "\"" + tagJson + "}";
    }

    private long postCount() {
        return jdbc.queryForObject("select count(*) from post p join category c using (category_id) where c.blog_id = ?",
                Long.class, owner.blogId());
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
