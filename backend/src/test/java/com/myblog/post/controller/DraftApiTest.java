package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.myblog.image.service.OrphanImageCleaner;
import com.myblog.support.TestImages;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.UUID;
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
 * 임시 저장 (specs/013 US3, S3-1 ~ S3-6, contracts 3). 임시 저장 글은 따로 표(post_draft)라 공개 글 쿼리에 나오지 않는다 (D-1).
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestImages.class})
class DraftApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrphanImageCleaner cleaner;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        session = members.login(mvc, owner);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    private static String draftBody(String title, String content, Long categoryId, Long topicId, Long seriesId,
            String visibility, String tagsJson) {
        String base = TestJson.of("title", title, "content", content, "categoryId", categoryId, "topicId", topicId,
                "seriesId", seriesId, "visibility", visibility);
        return base.substring(0, base.length() - 1) + ",\"tags\":" + tagsJson + "}";
    }

    private ResultActions createDraft(MockHttpSession who, String body) throws Exception {
        return mvc.perform(post("/api/v1/me/drafts").session(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Long newDraft(String title, String content) throws Exception {
        String body = createDraft(session, draftBody(title, content, null, null, null, "public", "[]"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.draftId")).longValue();
    }

    private ResultActions publish(Long draftId, String body) throws Exception {
        return mvc.perform(post("/api/v1/me/drafts/{id}/publish", draftId).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String fullBody(String title) {
        return draftBody(title, "본문", owner.defaultCategoryId(), topicId, null, "public", "[\"제주\"]");
    }

    @Nested
    @DisplayName("S3-1 주인만 본다")
    class OwnerOnly {

        @Test
        void 제목만_써도_저장되고_내_목록에_보인다() throws Exception {
            createDraft(session, draftBody("제목만", "", null, null, null, "public", "[]"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.draftId").isNumber())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                    .andExpect(jsonPath("$.publishAt").value(nullValue()));
            mvc.perform(get("/api/v1/me/drafts").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].title").value("제목만"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.maxCount").value(50));
        }

        @Test
        void 제목과_본문이_비어도_저장된다() throws Exception {
            newDraft("", "");
        }

        @Test
        void 남의_임시_저장_글은_404다() throws Exception {
            Long draftId = newDraft("비밀", "쓰는 중");
            MockHttpSession other = members.login(mvc, members.register("b"));
            mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(other)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DRAFT_NOT_FOUND"));
            mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(other).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(draftBody("뺏기", "", null, null, null, "public", "[]"))).andExpect(status().isNotFound());
            mvc.perform(delete("/api/v1/me/drafts/{id}", draftId).session(other).with(csrf())).andExpect(status().isNotFound());
            mvc.perform(post("/api/v1/me/drafts/{id}/publish", draftId).session(other).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(fullBody("뺏기"))).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/me/drafts").session(other)).andExpect(jsonPath("$.items", hasSize(0)));
        }

        @Test
        void 로그인하지_않으면_401이다() throws Exception {
            mvc.perform(get("/api/v1/me/drafts")).andExpect(status().isUnauthorized());
            createDraft(new MockHttpSession(), draftBody("", "", null, null, null, "public", "[]")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void S3_2_임시_저장_글은_목록_검색_첫_화면_잔디_매수에_나오지_않는다() throws Exception {
        newDraft("숨은 제주 이야기", "제주 본문");
        mvc.perform(get("/api/v1/blogs/{id}/posts", owner.blogId())).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/v1/blogs/{id}/posts", owner.blogId()).session(session)).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/v1/search/posts").param("q", "숨은 제주")).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/api/v1/blogs/{id}/manuscript", owner.blogId()).session(session)).andExpect(jsonPath("$.postCount").value(0));
        mvc.perform(get("/api/v1/blogs/{id}/activity", owner.blogId())).andExpect(jsonPath("$.totalPosts").value(0));
        mvc.perform(get("/api/v1/manage/posts").session(session)).andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void S3_3_다시_저장하면_새로_생기지_않고_고쳐진다() throws Exception {
        Long draftId = newDraft("처음", "");
        mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody("고친 제목", "고친 본문", owner.defaultCategoryId(), topicId, null, "private", "[\"제주\",\"여행\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").value(draftId));
        mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session))
                .andExpect(jsonPath("$.title").value("고친 제목"))
                .andExpect(jsonPath("$.content").value("고친 본문"))
                .andExpect(jsonPath("$.categoryId").value(owner.defaultCategoryId()))
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.visibility").value("private"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags[1]").value("여행"));
        mvc.perform(get("/api/v1/me/drafts").session(session)).andExpect(jsonPath("$.count").value(1));
    }

    @Nested
    @DisplayName("저장할 때의 검사 (글자 수 상한, 내 것인지)")
    class SaveRules {

        @Test
        void 제목_100자는_되고_101자는_거절한다() throws Exception {
            createDraft(session, draftBody("가".repeat(100), "", null, null, null, "public", "[]")).andExpect(status().isCreated());
            createDraft(session, draftBody("가".repeat(101), "", null, null, null, "public", "[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
        }

        @Test
        void 본문은_글_상한까지_된다() throws Exception {
            createDraft(session, draftBody("", "나".repeat(10_000), null, null, null, "public", "[]")).andExpect(status().isCreated());
            createDraft(session, draftBody("", "나".repeat(10_001), null, null, null, "public", "[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
        }

        @Test
        void 남의_분류나_남의_연재는_거절한다() throws Exception {
            Member other = members.register("b");
            createDraft(session, draftBody("", "", other.defaultCategoryId(), null, null, "public", "[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"));
            Long othersSeries = jdbc.queryForObject("insert into series (blog_id, name, created_at) values (?, '남의', now())"
                    + " returning series_id", Long.class, other.blogId());
            createDraft(session, draftBody("", "", null, null, othersSeries, "public", "[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("seriesId"));
        }

        @Test
        void 없는_주제와_모르는_공개_범위는_거절한다() throws Exception {
            createDraft(session, draftBody("", "", null, 999_999L, null, "public", "[]"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("topicId"));
            createDraft(session, draftBody("", "", null, null, null, "friends", "[]"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("visibility"));
        }

        @Test
        void 태그_칸이_너무_길면_거절한다() throws Exception {
            String longTag = "\"" + "다".repeat(201) + "\"";
            createDraft(session, draftBody("", "", null, null, null, "public", "[" + longTag + "]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("tags"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("DRAFT_TAGS_TOO_LONG"));
        }
    }

    @Nested
    @DisplayName("S3-4 발행")
    class Publishing {

        @Test
        void 발행하면_보통_글이_되고_임시_저장_글은_사라진다() throws Exception {
            Long draftId = newDraft("쓰던 글", "쓰던 본문");
            String body = publish(draftId, fullBody("다 쓴 글")).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long postId = ((Number) JsonPath.read(body, "$.postId")).longValue();
            mvc.perform(get("/api/v1/posts/{id}", postId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("다 쓴 글"))
                    .andExpect(jsonPath("$.tags[0]").value("제주"));
            mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session)).andExpect(status().isNotFound());
        }

        @Test
        void 발행할_때는_보통_글쓰기와_같은_검사를_하고_실패하면_임시_저장_글이_남는다() throws Exception {
            Long draftId = newDraft("", "");
            publish(draftId, draftBody("", "본문", owner.defaultCategoryId(), topicId, null, "public", "[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
            publish(draftId, draftBody("제목", "본문", owner.defaultCategoryId(), null, null, "public", "[]"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/me/drafts/{id}", draftId).session(session)).andExpect(status().isOk());
            assertThat(jdbc.queryForObject("select count(*) from post p join category c on c.category_id = p.category_id"
                    + " where c.blog_id = ?", Long.class, owner.blogId())).isZero();
        }

        @Test
        void 고른_연재에_끝_화로_들어간다() throws Exception {
            Long seriesId = jdbc.queryForObject("insert into series (blog_id, name, created_at) values (?, '연재', now())"
                    + " returning series_id", Long.class, owner.blogId());
            Long draftId = newDraft("", "");
            String body = publish(draftId, draftBody("1화", "본문", owner.defaultCategoryId(), topicId, seriesId, "public", "[]"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            Long postId = ((Number) JsonPath.read(body, "$.postId")).longValue();
            mvc.perform(get("/api/v1/posts/{id}/series", postId)).andExpect(jsonPath("$.series.seriesId").value(seriesId))
                    .andExpect(jsonPath("$.series.episode").value(1));
        }
    }

    @Test
    void S3_5_51번째_임시_저장은_거절한다() throws Exception {
        for (int i = 0; i < 49; i++) {
            jdbc.update("insert into post_draft (blog_id, request_key, created_at, updated_at) values (?, ?, now(), now())",
                    owner.blogId(), UUID.randomUUID().toString());
        }
        newDraft("50번째", "");
        createDraft(session, draftBody("51번째", "", null, null, null, "public", "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRAFT_LIMIT_EXCEEDED"));
    }

    @Test
    void 지우면_목록에서_사라진다() throws Exception {
        Long draftId = newDraft("지울 글", "");
        mvc.perform(delete("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/drafts").session(session)).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Nested
    @DisplayName("S3-6 이미지")
    class Images {

        private final String fileName = UUID.randomUUID() + ".png";
        private final String key = "posts/" + fileName;

        @BeforeEach
        void upload() {
            jdbc.update("insert into post_image (users_id, storage_key, created_at) values (?, ?, now() - interval '25 hours')",
                    owner.id(), key);
        }

        private Long draftIdOfImage() {
            return jdbc.queryForObject("select post_draft_id from post_image where storage_key = ?", Long.class, key);
        }

        private long imageCount() {
            Long count = jdbc.queryForObject("select count(*) from post_image where storage_key = ?", Long.class, key);
            return count == null ? 0 : count;
        }

        @Test
        void 임시_저장_글에_든_이미지는_하루가_지나도_지우지_않는다() throws Exception {
            Long draftId = newDraft("사진", "![](/api/images/" + fileName + ")");
            assertThat(draftIdOfImage()).isEqualTo(draftId);
            cleaner.cleanUp();
            assertThat(imageCount()).isOne();
        }

        @Test
        void 본문에서_뺀_이미지와_지운_임시_저장_글의_이미지는_정리_대상이_된다() throws Exception {
            Long draftId = newDraft("사진", "![](/api/images/" + fileName + ")");
            mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(draftBody("사진 뺌", "", null, null, null, "public", "[]"))).andExpect(status().isOk());
            assertThat(draftIdOfImage()).isNull();

            mvc.perform(put("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(draftBody("다시 넣음", "![](/api/images/" + fileName + ")", null, null, null, "public", "[]")))
                    .andExpect(status().isOk());
            mvc.perform(delete("/api/v1/me/drafts/{id}", draftId).session(session).with(csrf())).andExpect(status().isNoContent());
            assertThat(draftIdOfImage()).isNull();
            cleaner.cleanUp();
            assertThat(imageCount()).isZero();
        }

        @Test
        void 발행하면_이미지가_글로_옮겨_간다() throws Exception {
            Long draftId = newDraft("사진", "![](/api/images/" + fileName + ")");
            String body = publish(draftId, draftBody("사진 글", "![](/api/images/" + fileName + ")", owner.defaultCategoryId(),
                    topicId, null, "public", "[]")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            Long postId = ((Number) JsonPath.read(body, "$.postId")).longValue();
            assertThat(jdbc.queryForObject("select post_id from post_image where storage_key = ?", Long.class, key)).isEqualTo(postId);
            cleaner.cleanUp();
            assertThat(imageCount()).isOne();
        }

        @Test
        void 남의_이미지는_내_임시_저장_글에_붙지_않는다() throws Exception {
            jdbc.update("update post_image set users_id = ? where storage_key = ?", members.register("c").id(), key);
            newDraft("남의 사진", "![](/api/images/" + fileName + ")");
            assertThat(draftIdOfImage()).isNull();
        }
    }

    @Test
    void 글쓰기_설정에_자동_저장_간격이_온다() throws Exception {
        mvc.perform(get("/api/v1/me/blog/post-form").session(session))
                .andExpect(jsonPath("$.writing.draftAutosaveSeconds").value(30));
    }
}
