package com.myblog.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글결 MCP (specs/020 US2·US3, S2-1 ~ S3-2). JSON-RPC 메시지를 실제로 보내 프로토콜과 도구를 본다.
 * 다른 테스트의 글과 섞이지 않게 검색어에 테스트마다 다른 낱말을 쓴다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
@TestPropertySource(properties = "mcp.rate-limit.per-minute=40")
class McpTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.myblog.connect.service.McpRateLimiter rateLimiter;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redis;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member me;
    private Member other;
    private MockHttpSession mySession;
    private String word;
    private int nextId = 1;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        me = register("mc");
        other = register("mo");
        mySession = members.login(mvc, me);
        // 이 테스트에서만 나오는 낱말 (한글 + 무작위)
        word = "물안개" + UUID.randomUUID().toString().substring(0, 6);
    }

    @AfterEach
    void cleanUp() {
        for (Member member : created) {
            jdbc.update("delete from access_key where users_id = ?", member.id());
            jdbc.update("delete from post_draft where blog_id = ?", member.blogId());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private String key(String scope) throws Exception {
        String body = mvc.perform(post("/api/v1/me/access-keys").with(csrf()).session(mySession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "테스트", "scope", scope, "days", 30)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private Long write(Member writer, String title, String content, String visibility) {
        Long postId = members.addPost(writer.defaultCategoryId(), title, visibility);
        jdbc.update("update post set content = ? where post_id = ?", content, postId);
        return postId;
    }

    private MvcResult send(String token, String json) throws Exception {
        return mvc.perform(post("/api/v1/mcp").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).accept("application/json, text/event-stream").content(json))
                .andReturn();
    }

    private String rpc(String token, String method, String paramsJson) throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":" + (nextId++) + ",\"method\":\"" + method + "\""
                + (paramsJson == null ? "" : ",\"params\":" + paramsJson) + "}";
        MvcResult result = send(token, json);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String call(String token, String tool, String argumentsJson) throws Exception {
        return rpc(token, "tools/call", "{\"name\":\"" + tool + "\",\"arguments\":" + argumentsJson + "}");
    }

    @Test
    void 처음_인사와_알림_ping_모르는_메서드_잘못된_JSON_GET은_MCP_규칙대로_답한다() throws Exception {
        String token = key("read");
        String init = rpc(token, "initialize", "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}");
        assertThat((String) JsonPath.read(init, "$.jsonrpc")).isEqualTo("2.0");
        assertThat((Integer) JsonPath.read(init, "$.id")).isEqualTo(1);
        assertThat((String) JsonPath.read(init, "$.result.protocolVersion")).isEqualTo("2025-06-18");
        assertThat((String) JsonPath.read(init, "$.result.serverInfo.name")).isEqualTo("geulgyeol");
        assertThat((Map<?, ?>) JsonPath.read(init, "$.result.capabilities.tools")).isNotNull();
        assertThat((String) JsonPath.read(init, "$.result.instructions")).contains("글결").contains("출처");

        // 모르는 판을 달라고 하면 우리가 아는 판으로 답한다
        String older = rpc(token, "initialize", "{\"protocolVersion\":\"1999-01-01\",\"capabilities\":{}}");
        assertThat((String) JsonPath.read(older, "$.result.protocolVersion")).isEqualTo("2025-06-18");

        MvcResult notification = send(token, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        assertThat(notification.getResponse().getStatus()).isEqualTo(202);
        assertThat(notification.getResponse().getContentAsString()).isEmpty();

        assertThat((Map<?, ?>) JsonPath.read(rpc(token, "ping", null), "$.result")).isEmpty();
        String unknown = rpc(token, "resources/list", null);
        assertThat((Integer) JsonPath.read(unknown, "$.error.code")).isEqualTo(-32601);

        MvcResult broken = send(token, "{not json");
        assertThat(broken.getResponse().getStatus()).isEqualTo(400);
        assertThat((Integer) JsonPath.read(broken.getResponse().getContentAsString(), "$.error.code")).isEqualTo(-32700);
        MvcResult notRpc = send(token, "{\"jsonrpc\":\"1.0\",\"id\":3,\"method\":\"ping\"}");
        assertThat((Integer) JsonPath.read(notRpc.getResponse().getContentAsString(), "$.error.code")).isEqualTo(-32600);
        // 요청 id는 글자나 정수만 (null·참거짓·소수는 잘못된 요청)
        for (String badId : List.of("null", "true", "1.5")) {
            MvcResult bad = send(token, "{\"jsonrpc\":\"2.0\",\"id\":" + badId + ",\"method\":\"tools/list\"}");
            assertThat(bad.getResponse().getStatus()).as("id %s", badId).isEqualTo(400);
            assertThat((Integer) JsonPath.read(bad.getResponse().getContentAsString(), "$.error.code")).isEqualTo(-32600);
        }
        assertThat((String) JsonPath.read(send(token, "{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"ping\"}")
                .getResponse().getContentAsString(), "$.id")).isEqualTo("abc");

        int getStatus = mvc.perform(get("/api/v1/mcp").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse().getStatus();
        assertThat(getStatus).isEqualTo(405);
        int deleteStatus = mvc.perform(delete("/api/v1/mcp").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse().getStatus();
        assertThat(deleteStatus).isEqualTo(405);
    }

    @Test
    void 읽기만_열쇠에는_임시_저장_도구가_없고_불러도_거절된다() throws Exception {
        String read = key("read");
        List<String> readTools = JsonPath.read(rpc(read, "tools/list", null), "$.result.tools[*].name");
        assertThat(readTools).containsExactlyInAnyOrder("search_posts", "search_passages", "read_post", "list_my_posts",
                "my_blog_stats");
        String refused = call(read, "save_draft", "{\"title\":\"몰래\",\"content\":\"본문\"}");
        assertThat((Boolean) JsonPath.read(refused, "$.result.isError")).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from post_draft where blog_id = ?", Long.class, me.blogId()))
                .isZero();

        String draft = key("draft");
        String list = rpc(draft, "tools/list", null);
        List<String> draftTools = JsonPath.read(list, "$.result.tools[*].name");
        assertThat(draftTools).contains("save_draft").hasSize(6);
        // 도구마다 입력 모양과 읽기 전용 여부를 알린다
        assertThat((String) JsonPath.read(list, "$.result.tools[?(@.name=='search_passages')].inputSchema.type")
                .toString()).contains("object");
        assertThat(JsonPath.read(list, "$.result.tools[?(@.name=='search_posts')].annotations.readOnlyHint").toString())
                .contains("true");
        assertThat(JsonPath.read(list, "$.result.tools[?(@.name=='save_draft')].annotations.readOnlyHint").toString())
                .contains("false");
        String unknownTool = call(draft, "delete_post", "{\"postId\":1}");
        assertThat((Integer) JsonPath.read(unknownTool, "$.error.code")).isEqualTo(-32602);
    }

    @Test
    void S2_1_글_찾기는_공개_글과_내_글만_찾고_남의_비공개_글은_없다() throws Exception {
        Long mine = write(me, word + " 내 비공개 메모", "혼자 보는 " + word + " 기록", "private");
        Long theirs = write(other, word + " 남의 공개 글", "모두가 보는 " + word + " 이야기", "public");
        Long hidden = write(other, word + " 남의 비공개 글", "숨긴 " + word + " 이야기", "private");
        String token = key("read");

        String found = call(token, "search_posts", "{\"query\":\"" + word + "\"}");
        assertThat((Boolean) JsonPath.read(found, "$.result.isError")).isFalse();
        List<Integer> ids = JsonPath.read(found, "$.result.structuredContent.posts[*].postId");
        assertThat(ids.stream().map(Integer::longValue).toList()).containsExactlyInAnyOrder(mine, theirs)
                .doesNotContain(hidden);
        assertThat((String) JsonPath.read(found, "$.result.content[0].type")).isEqualTo("text");
        assertThat((String) JsonPath.read(found, "$.result.content[0].text")).contains("/posts/" + theirs)
                .doesNotContain("숨긴");

        String onlyMine = call(token, "search_posts", "{\"query\":\"" + word + "\",\"mineOnly\":true}");
        List<Integer> mineIds = JsonPath.read(onlyMine, "$.result.structuredContent.posts[*].postId");
        assertThat(mineIds.stream().map(Integer::longValue).toList()).containsExactly(mine);

        String tooShort = call(token, "search_posts", "{\"query\":\"가\"}");
        assertThat((Boolean) JsonPath.read(tooShort, "$.result.isError")).isTrue();
        String missing = call(token, "search_posts", "{}");
        assertThat((Boolean) JsonPath.read(missing, "$.result.isError")).isTrue();
    }

    @Test
    void S2_RAG_단락_찾기는_맞는_단락을_출처와_함께_글마다_두_개까지_돌려준다() throws Exception {
        String bird = "청둥" + UUID.randomUUID().toString().substring(0, 4);
        String content = String.join("\n\n",
                "# 들어가며",
                "오늘은 날씨 이야기를 조금 했다.",
                "새벽 호숫가에는 " + word + "이 피어올랐다. " + word + " 사이로 " + bird + "가 지나갔다.",
                "![사진](/api/images/x.png)",
                "점심에는 국수를 먹었다.",
                "돌아오는 길에도 " + word + "이 남아 있었다.",
                "마지막으로 " + word + " 사진을 정리했다.");
        Long postId = write(me, "호수 산책 기록", content, "public");
        write(other, word + " 남의 비공개", "숨긴 " + word + " 단락", "private");
        String token = key("read");

        String result = call(token, "search_passages", "{\"query\":\"" + word + " " + bird + "\"}");
        assertThat((Boolean) JsonPath.read(result, "$.result.isError")).isFalse();
        List<Map<String, Object>> passages = JsonPath.read(result, "$.result.structuredContent.passages");
        assertThat(passages).hasSize(2);
        Map<String, Object> best = passages.get(0);
        assertThat(((Number) best.get("postId")).longValue()).isEqualTo(postId);
        assertThat((String) best.get("text")).contains(bird + "가 지나갔다");
        assertThat((String) best.get("url")).endsWith("/posts/" + postId);
        // "# 들어가며"는 다음 단락에 붙어 1번째, 맞은 단락은 2번째
        assertThat(((Number) best.get("paragraph")).intValue()).isEqualTo(2);
        assertThat((String) best.get("title")).isEqualTo("호수 산책 기록");
        assertThat(result).doesNotContain("숨긴").doesNotContain("/api/images/x.png");
        // 사람이 읽는 글에도 출처가 붙는다
        assertThat((String) JsonPath.read(result, "$.result.content[0].text")).contains("호수 산책 기록")
                .contains("/posts/" + postId);
    }

    @Test
    void 글_읽기와_내_글_목록과_내_통계() throws Exception {
        Long mine = write(me, "내 비공개 원고", "# 제목\n\n비밀 **본문**", "private");
        Long hidden = write(other, "남의 비공개", "숨긴 본문", "private");
        String token = key("read");

        String read = call(token, "read_post", "{\"postId\":" + mine + "}");
        assertThat((String) JsonPath.read(read, "$.result.structuredContent.content")).isEqualTo("# 제목\n\n비밀 **본문**");
        assertThat((String) JsonPath.read(read, "$.result.structuredContent.visibility")).isEqualTo("private");
        String denied = call(token, "read_post", "{\"postId\":" + hidden + "}");
        assertThat((Boolean) JsonPath.read(denied, "$.result.isError")).isTrue();
        assertThat(denied).doesNotContain("숨긴 본문");

        String myPosts = call(token, "list_my_posts", "{}");
        List<Integer> ids = JsonPath.read(myPosts, "$.result.structuredContent.posts[*].postId");
        assertThat(ids.stream().map(Integer::longValue).toList()).contains(mine).doesNotContain(hidden);

        String stats = call(token, "my_blog_stats", "{}");
        assertThat((Boolean) JsonPath.read(stats, "$.result.isError")).isFalse();
        assertThat((Integer) JsonPath.read(stats, "$.result.structuredContent.views.total")).isNotNull();
    }

    @Test
    void S3_AI가_쓴_초안은_임시_저장까지만_간다() throws Exception {
        String token = key("draft");
        String saved = call(token, "save_draft", "{\"title\":\"AI가 쓴 초안\",\"content\":\"## 첫 문단\\n\\n고쳐 쓸 글\"}");
        assertThat((Boolean) JsonPath.read(saved, "$.result.isError")).isFalse();
        Number draftId = JsonPath.read(saved, "$.result.structuredContent.draftId");
        assertThat((String) JsonPath.read(saved, "$.result.structuredContent.editUrl")).contains(String.valueOf(draftId));
        assertThat(jdbc.queryForObject("select title from post_draft where post_draft_id = ?", String.class,
                draftId.longValue())).isEqualTo("AI가 쓴 초안");
        // 글로 올라가지 않았다
        assertThat(jdbc.queryForObject("select count(*) from post p join category c on c.category_id = p.category_id"
                + " where c.blog_id = ? and p.title = 'AI가 쓴 초안'", Long.class, me.blogId())).isZero();

        String tooLong = call(token, "save_draft", "{\"title\":\"" + "가".repeat(101) + "\",\"content\":\"본문\"}");
        assertThat((Boolean) JsonPath.read(tooLong, "$.result.isError")).isTrue();
    }

    @Test
    void S2_3_열쇠마다_요청_수를_넘으면_429다() throws Exception {
        String token = key("read");
        int status = 200;
        for (int i = 0; i < 41 && status == 200; i++) {
            status = send(token, "{\"jsonrpc\":\"2.0\",\"id\":" + i + ",\"method\":\"ping\"}").getResponse().getStatus();
        }
        assertThat(status).isEqualTo(429);
        // 다른 열쇠는 따로 센다
        String another = key("read");
        assertThat(send(another, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void 요청_수_키에_만료가_빠져_있어도_다시_걸어_영원히_막히지_않는다() {
        long keyId = -System.nanoTime();   // 실제 열쇠와 겹치지 않는 번호
        String key = "myblog:mcp:rate:" + keyId;
        try {
            // 예전처럼 INCR만 되고 EXPIRE가 빠진 상태
            redis.opsForValue().set(key, "100");
            assertThat(redis.getExpire(key)).isEqualTo(-1L);
            assertThat(rateLimiter.allow(keyId)).isFalse();
            assertThat(redis.getExpire(key)).isBetween(1L, 60L);
        } finally {
            redis.delete(key);
        }
        // 새 키는 첫 요청에서 1분 창이 열린다
        try {
            assertThat(rateLimiter.allow(keyId)).isTrue();
            assertThat(redis.getExpire(key)).isBetween(1L, 60L);
        } finally {
            redis.delete(key);
        }
    }
}
