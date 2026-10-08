package com.myblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.image.storage.InMemoryImageStorage;
import com.myblog.support.TestComments;
import com.myblog.support.TestImages;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * 글 삭제 연쇄 전체 (specs/005 T064, quickstart S-11, FR-006, FR-027, FR-028, SC-005).
 * 댓글 2, 좋아요 2, 태그 3, 이미지 3, 신고 1이 달린 글을 지우면 모두 0건이고, 태그 줄은 남는다.
 * 탈퇴하면 그 회원의 글에 딸린 것도 같은 길(PostDeletingEvent)로 모두 지워진다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestImages.class})
class PostDeletionCascadeTest {

    private static final Map<String, String> CHILD_TABLES = Map.of(
            "comment", "post_id", "post_like", "post_id", "post_tag", "post_id", "post_image", "post_id",
            "post_report", "post_id");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private InMemoryImageStorage storage;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private MockHttpSession ownerSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        storage.reset();
        owner = register("o");
        ownerSession = members.login(mvc, owner);
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S11_딸린_것이_모두_있는_글을_지우면_모두_0건이고_파일도_없다() throws Exception {
        Long postId = fullPost(ownerSession, owner);
        for (String table : CHILD_TABLES.keySet()) {
            assertThat(count(table, postId)).as(table).isPositive();
        }
        List<String> keys = jdbc.queryForList("select storage_key from post_image where post_id = ?", String.class, postId);

        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());

        for (String table : CHILD_TABLES.keySet()) {
            assertThat(count(table, postId)).as(table).isZero();
        }
        assertThat(keys).hasSize(3).noneMatch(storage::has);
        assertThat(jdbc.queryForObject("select count(*) from tag where name like ?", Long.class, "cascade%")).isEqualTo(3);
    }

    @Test
    void 탈퇴하면_그_회원의_글에_딸린_것도_모두_지워진다() throws Exception {
        Long postId = fullPost(ownerSession, owner);
        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from post where post_id = ?", Long.class, postId)).isZero();
        for (String table : CHILD_TABLES.keySet()) {
            assertThat(count(table, postId)).as(table).isZero();
        }
    }

    /** 댓글 2, 좋아요 2, 태그 3, 이미지 3, 신고 1이 달린 공개 글. */
    private Long fullPost(MockHttpSession session, Member writer) throws Exception {
        StringBuilder content = new StringBuilder("본문\n\n");
        for (int i = 0; i < 3; i++) {
            String url = JsonPath.read(mvc.perform(multipart("/api/images").file(TestImages.png()).with(csrf()).session(session))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.url");
            content.append("![](").append(url).append(")\n\n");
        }
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        String body = "{\"title\":\"전부 달린 글\",\"content\":" + TestJson.quote(content.toString())
                + ",\"categoryId\":" + writer.defaultCategoryId() + ",\"topicId\":" + topicId + ",\"tags\":[\"cascade1\",\"cascade2\",\"cascade3\"]}";
        Long postId = ((Number) JsonPath.read(mvc.perform(post("/api/posts").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.postId")).longValue();

        for (int i = 0; i < 2; i++) {
            Member reader = register("r");
            MockHttpSession readerSession = members.login(mvc, reader);
            mvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).session(readerSession)
                    .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "댓글"))).andExpect(status().isCreated());
            mvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).session(readerSession)).andExpect(status().isOk());
            if (i == 0) {
                mvc.perform(post("/api/posts/{id}/reports", postId).with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "SPAM")))
                        .andExpect(status().isCreated());
            }
        }
        return postId;
    }

    private long count(String table, Long postId) {
        // 표 이름은 위의 고정 목록에서만 온다 (요청 값이 아님)
        return jdbc.queryForObject("select count(*) from " + table + " where " + CHILD_TABLES.get(table) + " = ?",
                Long.class, postId);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
