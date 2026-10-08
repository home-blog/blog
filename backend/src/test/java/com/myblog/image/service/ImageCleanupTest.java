package com.myblog.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.image.storage.InMemoryImageStorage;
import com.myblog.support.TestComments;
import com.myblog.support.TestImages;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 이미지 연결과 정리 (specs/005 US4, T048, quickstart S-11의 이미지 줄, S-11의 10, FR-024, FR-027, SC-005, D-3, D-5).
 * 커밋 뒤의 파일 삭제를 보므로 클래스 전체 @Transactional을 쓰지 않는다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestImages.class})
class ImageCleanupTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private InMemoryImageStorage storage;

    @Autowired
    private OrphanImageCleaner cleaner;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member writer;
    private MockHttpSession session;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        storage.reset();
        writer = register("w");
        session = members.login(mvc, writer);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    @AfterEach
    void cleanUp() {
        storage.reset();
        created.forEach(testComments::deleteMember);
    }

    @Test
    void D3_글을_저장하면_본문에_든_내_이미지만_그_글에_연결된다() throws Exception {
        String a = upload(session);
        String b = upload(session);
        String unused = upload(session);
        Member other = register("o");
        String othersImage = upload(members.login(mvc, other));
        Long postId = write("![첫](" + a + ")\n본문\n![둘](" + b + ")\n![남의](" + othersImage + ")\n[링크](" + unused + ")");

        assertThat(linkedTo(postId)).containsExactlyInAnyOrder(key(a), key(b));
        assertThat(postIdOf(unused)).isNull();
        assertThat(postIdOf(othersImage)).isNull();
        // 연결되면 그 글을 볼 수 있는 사람은 누구나 본다
        mvc.perform(get(a)).andExpect(status().isOk());
    }

    @Test
    void FR024_본문_속_이미지가_10장을_넘으면_저장_전체를_거절한다() throws Exception {
        StringBuilder content = new StringBuilder("본문\n");
        for (int i = 0; i < 9; i++) {
            content.append("![](").append(upload(session)).append(")\n");
        }
        Long postId = write(content.toString());
        // 올리기는 10장에서 막으므로(S-7의 7), 다른 경로로 생긴 연결 전 이미지 2장을 바로 넣어 이 글의 9장 + 2장 = 11장을 만든다
        String tenth = "/api/images/00000000-0000-0000-0000-0000000000a1.png";
        String eleventh = "/api/images/00000000-0000-0000-0000-0000000000a2.png";
        for (String url : List.of(tenth, eleventh)) {
            jdbc.update("insert into post_image (users_id, storage_key, created_at) values (?, ?, now())", writer.id(), key(url));
        }
        long before = jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, postId);
        updateContent(postId, content + "![](" + tenth + ")\n![](" + eleventh + ")")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("IMAGE_LIMIT_EXCEEDED"));
        assertThat(before).isOne();
        assertThat(linkedTo(postId)).hasSize(9);
        assertThat(jdbc.queryForObject("select content from post where post_id = ?", String.class, postId))
                .isEqualTo(content.toString());
    }

    @Test
    void 수정에서_본문에서_뺀_이미지는_기록과_파일이_지워진다() throws Exception {
        String a = upload(session);
        String b = upload(session);
        Long postId = write("![](" + a + ")\n![](" + b + ")");
        updateContent(postId, "![](" + a + ")\n둘째 이미지를 뺐다").andExpect(status().isOk());
        assertThat(linkedTo(postId)).containsExactly(key(a));
        assertThat(storage.has(key(b))).isFalse();
        assertThat(storage.has(key(a))).isTrue();
    }

    @Test
    void S11_글을_지우면_기록과_파일이_지워지고_주소는_404() throws Exception {
        String a = upload(session);
        String b = upload(session);
        Long postId = write("![](" + a + ")\n![](" + b + ")");
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(session)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from post_image where post_id = ?", Long.class, postId)).isZero();
        assertThat(storage.has(key(a)) || storage.has(key(b))).isFalse();
        mvc.perform(get(a).session(session)).andExpect(status().isNotFound());
    }

    @Test
    void D5_파일_삭제가_실패해도_글은_지워지고_정리_작업이_나중에_파일을_지운다() throws Exception {
        String a = upload(session);
        Long postId = write("![](" + a + ")");
        storage.failDelete = true;
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(session)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, postId)).isZero();
        assertThat(storage.has(key(a))).isTrue();

        storage.failDelete = false;
        cleaner.cleanUp();
        assertThat(storage.has(key(a))).as("올린 지 얼마 안 된 파일은 정리 시간이 지나야 지운다").isTrue();
        storage.age(key(a), Instant.now().minus(25, ChronoUnit.HOURS));
        cleaner.cleanUp();
        assertThat(storage.has(key(a))).isFalse();
    }

    @Test
    void S11_10_저장하지_않은_이미지는_정리_시간이_지나면_기록과_파일이_지워진다() throws Exception {
        String old = upload(session);
        String fresh = upload(session);
        jdbc.update("update post_image set created_at = now() - interval '25 hours' where storage_key = ?", key(old));
        cleaner.cleanUp();
        assertThat(jdbc.queryForObject("select count(*) from post_image where storage_key = ?", Long.class, key(old))).isZero();
        assertThat(storage.has(key(old))).isFalse();
        assertThat(storage.has(key(fresh))).isTrue();
        assertThat(postIdOf(fresh)).isNull();
    }

    @Test
    void 저장소가_꺼져_있어도_정리_작업은_멈추지_않는다() {
        storage.failAll = true;
        cleaner.cleanUp();
    }

    private String upload(MockHttpSession who) throws Exception {
        return JsonPath.read(mvc.perform(multipart("/api/images").file(TestImages.png()).with(csrf()).session(who))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.url");
    }

    private Long write(String content) throws Exception {
        String response = mvc.perform(post("/api/posts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", "이미지 글", "content", content, "categoryId", writer.defaultCategoryId(),
                                "topicId", topicId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.postId")).longValue();
    }

    private ResultActions updateContent(Long postId, String content) throws Exception {
        return mvc.perform(put("/api/posts/{id}", postId).with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("title", "이미지 글", "content", content, "categoryId", writer.defaultCategoryId(),
                        "topicId", topicId, "visibility", "public")));
    }

    private List<String> linkedTo(Long postId) {
        return jdbc.queryForList("select storage_key from post_image where post_id = ?", String.class, postId);
    }

    private Long postIdOf(String url) {
        return jdbc.queryForObject("select post_id from post_image where storage_key = ?", Long.class, key(url));
    }

    private static String key(String url) {
        return "posts/" + url.substring("/api/images/".length());
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
