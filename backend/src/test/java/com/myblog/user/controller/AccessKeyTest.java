package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
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
 * 개인 열쇠 (specs/020 US1, S1-1 ~ S1-4). 열쇠는 MCP 주소에서만 로그인으로 통한다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class AccessKeyTest {

    private static final String PING = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";

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
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("ak");
        session = members.login(mvc, owner);
    }

    @AfterEach
    void cleanUp() {
        for (Member member : created) {
            jdbc.update("delete from access_key where users_id = ?", member.id());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private ResultActions create(MockHttpSession who, Object... fields) throws Exception {
        return mvc.perform(post("/api/v1/me/access-keys").with(csrf()).session(who)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of(fields)));
    }

    private String createToken(String name, String scope) throws Exception {
        String body = create(session, "name", name, "scope", scope, "days", 30).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private ResultActions mcp(String token, String body) throws Exception {
        var request = post("/api/v1/mcp").contentType(MediaType.APPLICATION_JSON).accept("application/json, text/event-stream")
                .content(body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void S1_1_열쇠는_만들_때_한_번만_보이고_해시만_저장된다() throws Exception {
        String body = create(session, "name", "Claude Code", "scope", "draft", "days", 90)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Claude Code"))
                .andExpect(jsonPath("$.scope").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = JsonPath.read(body, "$.token");
        assertThat(token).startsWith("gk_").hasSizeGreaterThan(40);
        String hint = JsonPath.read(body, "$.hint");
        assertThat(token).startsWith(hint);

        String stored = jdbc.queryForObject("select token_hash from access_key where users_id = ?", String.class, owner.id());
        assertThat(stored).isEqualTo(sha256(token)).doesNotContain(token);

        String list = mvc.perform(get("/api/v1/me/access-keys").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].hint").value(hint))
                .andExpect(jsonPath("$.maxCount").value(5))
                .andExpect(jsonPath("$.nameMaxLength").value(40))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(list).doesNotContain(token);
        assertThat((Object) JsonPath.read(list, "$.items[0].lastUsedAt")).isNull();

        // 써 보면 마지막 사용 시각이 남는다
        mcp(token, PING).andExpect(status().isOk()).andExpect(jsonPath("$.result").isMap());
        mvc.perform(get("/api/v1/me/access-keys").session(session))
                .andExpect(jsonPath("$.items[0].lastUsedAt").isNotEmpty());
    }

    @Test
    void S1_2_틀린_열쇠_기한이_지난_열쇠_끊은_열쇠_탈퇴한_회원의_열쇠는_401이다() throws Exception {
        mcp(null, PING).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
        mcp("gk_" + "x".repeat(43), PING).andExpect(status().isUnauthorized());

        String expired = createToken("기한 지남", "read");
        jdbc.update("update access_key set created_at = now() - interval '31 days', expires_at = now() - interval '1 minute' where token_hash = ?", sha256(expired));
        mcp(expired, PING).andExpect(status().isUnauthorized());

        String revoked = createToken("끊을 열쇠", "read");
        mcp(revoked, PING).andExpect(status().isOk());
        Long keyId = jdbc.queryForObject("select access_key_id from access_key where token_hash = ?", Long.class,
                sha256(revoked));
        mvc.perform(delete("/api/v1/me/access-keys/{id}", keyId).with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mcp(revoked, PING).andExpect(status().isUnauthorized());

        String withdrawn = createToken("탈퇴 전", "read");
        jdbc.update("update users set deleted_at = now() where users_id = ?", owner.id());
        mcp(withdrawn, PING).andExpect(status().isUnauthorized());
        jdbc.update("update users set deleted_at = null where users_id = ?", owner.id());
    }

    @Test
    void S1_3_열쇠는_MCP_주소에서만_통하고_세션도_만들지_않는다() throws Exception {
        String token = createToken("다른 곳에 써 보기", "draft");
        mvc.perform(get("/api/v1/me/access-keys").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("title", "열쇠로 쓰기")))
                .andExpect(status().isForbidden());
        var result = mcp(token, PING).andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getCookies()).isEmpty();
        // 세션 쿠키로는 MCP에 붙지 않는다 (열쇠만)
        mvc.perform(post("/api/v1/mcp").session(session).contentType(MediaType.APPLICATION_JSON).content(PING))
                .andExpect(status().isUnauthorized());
        // 다른 사이트에서 브라우저가 보낸 요청은 막는다
        mcp(token, PING).andExpect(status().isOk());
        mvc.perform(post("/api/v1/mcp").header("Authorization", "Bearer " + token).header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON).content(PING))
                .andExpect(status().isForbidden());
    }

    @Test
    void S1_4_이름_범위_기한을_검사하고_다섯_개까지만_만들며_남의_열쇠는_끊을_수_없다() throws Exception {
        create(session, "name", " ", "scope", "read", "days", 30).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCESS_KEY_INVALID"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1~40자")));
        create(session, "name", "가".repeat(41), "scope", "read", "days", 30).andExpect(status().isBadRequest());
        create(session, "name", "범위", "scope", "admin", "days", 30).andExpect(status().isBadRequest());
        create(session, "name", "기한", "scope", "read", "days", 7).andExpect(status().isBadRequest());
        create(session, "name", "가".repeat(40), "scope", "read", "days", 365).andExpect(status().isCreated());
        for (int i = 2; i <= 5; i++) {
            create(session, "name", "열쇠 " + i, "scope", "read", "days", 30).andExpect(status().isCreated());
        }
        create(session, "name", "여섯째", "scope", "read", "days", 30).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCESS_KEY_LIMIT"));

        Member other = register("ao");
        MockHttpSession otherSession = members.login(mvc, other);
        Long mine = jdbc.queryForObject("select min(access_key_id) from access_key where users_id = ?", Long.class,
                owner.id());
        mvc.perform(delete("/api/v1/me/access-keys/{id}", mine).with(csrf()).session(otherSession))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ACCESS_KEY_NOT_FOUND"));
        mvc.perform(get("/api/v1/me/access-keys")).andExpect(status().isUnauthorized());
    }

    @Test
    void S1_5_비밀번호를_바꾸면_열쇠가_모두_끊긴다() throws Exception {
        String token = createToken("바꾸기 전", "draft");
        mcp(token, PING).andExpect(status().isOk());
        mvc.perform(post("/api/v1/account/password").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("currentPassword", TestMembers.PASSWORD, "newPassword", "efgh5678!",
                                "newPasswordConfirm", "efgh5678!")))
                .andExpect(status().isOk());
        mcp(token, PING).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from access_key where users_id = ?", Integer.class, owner.id()))
                .isZero();
    }
}
