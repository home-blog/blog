package com.myblog.user.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.user.service.AccessKeyService;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글결 MCP OAuth (specs/023 S1-1 ~ S2-3). 등록 → 인가 → 위임장 허락 → 토큰 → MCP를 실제 요청으로 걷는다.
 * 등록 횟수는 IP마다 세므로 요청마다 다른 가짜 IP를 쓴다(한 시간 동안 남는 Redis 값이 다음 실행과 겹치지 않게).
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
@TestPropertySource(properties = {"mcp.oauth.issuer=", "mcp.oauth.register-per-hour=3",
        "mcp.oauth.max-grants-per-member=3"})
class OAuthTest {

    private static final String CALLBACK = "https://claude.ai/api/mcp/auth_callback";
    private static final String ISSUER = "http://localhost";
    private static final String RESOURCE = ISSUER + "/api/v1/mcp";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final tools.jackson.databind.json.JsonMapper JSON = new tools.jackson.databind.json.JsonMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AccessKeyService accessKeys;

    @Autowired
    private TestClock clock;

    @Autowired
    private OAuthService oauthService;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private final List<String> clients = new ArrayList<>();
    private Member me;
    private MockHttpSession mySession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        me = register("oa");
        mySession = members.login(mvc, me);
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        for (Member member : created) {
            jdbc.update("delete from oauth_grant where users_id = ?", member.id());
            testComments.deleteMember(member);
        }
        clients.forEach(id -> jdbc.update("delete from oauth_client where client_id = ?", id));
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private static String randomIp() {
        return "10." + RANDOM.nextInt(256) + "." + RANDOM.nextInt(256) + "." + RANDOM.nextInt(256);
    }

    private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder request, String ip) {
        return request.with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
    }

    private ResultActions registerClient(String ip, Object... fields) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i += 2) {
            body.put((String) fields[i], fields[i + 1]);
        }
        return mvc.perform(from(post("/oauth/register"), ip).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)));
    }

    private String newClient(String... redirectUris) throws Exception {
        String body = registerClient(randomIp(), "client_name", "Claude", "redirect_uris",
                redirectUris.length == 0 ? List.of(CALLBACK) : List.of(redirectUris))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String clientId = JsonPath.read(body, "$.client_id");
        clients.add(clientId);
        return clientId;
    }

    private static String verifier() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String challenge(String verifier) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    }

    private static Map<String, String> query(String location) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = URI.create(location).getRawQuery();
        if (raw == null) {
            return values;
        }
        for (String pair : raw.split("&")) {
            int at = pair.indexOf('=');
            values.put(URLDecoder.decode(pair.substring(0, at), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(at + 1), StandardCharsets.UTF_8));
        }
        return values;
    }

    private String authorize(Map<String, String> params) throws Exception {
        var request = get("/oauth/authorize");
        params.forEach(request::param);
        MvcResult result = mvc.perform(request).andExpect(status().isFound()).andReturn();
        return result.getResponse().getHeader("Location");
    }

    private Map<String, String> authParams(String clientId, String verifier) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", clientId);
        params.put("redirect_uri", CALLBACK);
        params.put("code_challenge", challenge(verifier));
        params.put("code_challenge_method", "S256");
        params.put("state", "상태 값+1");
        params.put("resource", RESOURCE);
        return params;
    }

    private String requestId(String location) {
        assertThat(location).startsWith("/connect/authorize?request=");
        return location.substring("/connect/authorize?request=".length());
    }

    private String approve(MockHttpSession session, String requestId, String scope) throws Exception {
        String body = mvc.perform(post("/api/v1/oauth/requests/{id}/approve", requestId).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("scope", scope)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.redirectTo");
    }

    private ResultActions token(String... pairs) throws Exception {
        var request = post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED);
        for (int i = 0; i < pairs.length; i += 2) {
            request.param(pairs[i], pairs[i + 1]);
        }
        return mvc.perform(request);
    }

    /** 처음부터 끝까지: 등록 → 인가 → 허락 → 토큰. */
    private record Connected(String clientId, String access, String refresh, String code, String verifier) {
    }

    private Connected connect(MockHttpSession session, String scope) throws Exception {
        String clientId = newClient();
        String verifier = verifier();
        String requestId = requestId(authorize(authParams(clientId, verifier)));
        Map<String, String> back = query(approve(session, requestId, scope));
        String body = token("grant_type", "authorization_code", "code", back.get("code"), "client_id", clientId,
                "redirect_uri", CALLBACK, "code_verifier", verifier, "resource", RESOURCE)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Connected(clientId, JsonPath.read(body, "$.access_token"), JsonPath.read(body, "$.refresh_token"),
                back.get("code"), verifier);
    }

    private MvcResult mcp(String token, String method) throws Exception {
        var request = post("/api/v1/mcp").contentType(MediaType.APPLICATION_JSON)
                .accept("application/json, text/event-stream")
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\"}");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn();
    }

    @Test
    void S1_1_MCP는_출입증이_없으면_401과_보호_자원_메타데이터_주소를_알리고_메타데이터는_누구나_읽는다() throws Exception {
        MvcResult unauthenticated = mcp(null, "tools/list");
        assertThat(unauthenticated.getResponse().getStatus()).isEqualTo(401);
        assertThat(unauthenticated.getResponse().getHeader("WWW-Authenticate")).contains("realm=\"geulgyeol\"")
                .contains("resource_metadata=\"" + ISSUER + "/.well-known/oauth-protected-resource/api/v1/mcp\"");

        for (String path : List.of("/.well-known/oauth-protected-resource/api/v1/mcp",
                "/.well-known/oauth-protected-resource")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource").value(RESOURCE))
                    .andExpect(jsonPath("$.authorization_servers[0]").value(ISSUER))
                    .andExpect(jsonPath("$.scopes_supported[1]").value("draft"));
        }
        mvc.perform(get("/.well-known/oauth-authorization-server")).andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint").value(ISSUER + "/oauth/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value(ISSUER + "/oauth/token"))
                .andExpect(jsonPath("$.registration_endpoint").value(ISSUER + "/oauth/register"))
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"))
                .andExpect(jsonPath("$.token_endpoint_auth_methods_supported[0]").value("none"));
        // 브라우저에서 도는 MCP 클라이언트도 부를 수 있다 (쿠키 없이 모든 출처)
        mvc.perform(options("/oauth/token").header("Origin", "https://inspector.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        // 세션 쿠키를 만들지 않는다
        assertThat(mvc.perform(get("/.well-known/oauth-authorization-server")).andReturn().getResponse()
                .getHeader("Set-Cookie")).isNull();
    }

    @Test
    void S1_2_등록은_https나_루프백_주소만_받고_이름이_없으면_이름_없는_앱이다() throws Exception {
        String ip = randomIp();
        registerClient(ip, "client_name", "  ", "redirect_uris", List.of("http://localhost:33418/callback"),
                "token_endpoint_auth_method", "client_secret_basic")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").isString())
                .andExpect(jsonPath("$.client_name").value("이름 없는 앱"))
                .andExpect(jsonPath("$.token_endpoint_auth_method").value("none"))
                .andExpect(jsonPath("$.client_secret").doesNotExist())
                .andDo(result -> clients.add(JsonPath.read(result.getResponse().getContentAsString(), "$.client_id")));
        for (Object bad : List.of(List.of("http://evil.example/cb"), List.of("https://claude.ai/cb#frag"),
                List.of("not a uri"), List.of(), List.of("https://a.example/1", "https://a.example/2",
                        "https://a.example/3", "https://a.example/4", "https://a.example/5", "https://a.example/6"))) {
            registerClient(randomIp(), "client_name", "x", "redirect_uris", bad).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_redirect_uri"));
        }
        registerClient(randomIp(), "client_name", "가".repeat(101), "redirect_uris", List.of(CALLBACK))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_client_metadata"));
        registerClient(randomIp(), "client_name", "x", "redirect_uris", List.of(CALLBACK), "grant_types",
                List.of("client_credentials")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_client_metadata"));
    }

    @Test
    void S1_2_한_IP의_등록은_한_시간에_정한_수까지() throws Exception {
        String ip = randomIp();
        for (int i = 0; i < 3; i++) {
            registerClient(ip, "client_name", "앱" + i, "redirect_uris", List.of(CALLBACK))
                    .andExpect(status().isCreated())
                    .andDo(result -> clients.add(JsonPath.read(result.getResponse().getContentAsString(), "$.client_id")));
        }
        registerClient(ip, "client_name", "넷째", "redirect_uris", List.of(CALLBACK))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void S1_처음부터_끝까지_허락하면_앱이_출입증으로_MCP를_쓴다() throws Exception {
        String clientId = newClient();
        String verifier = verifier();
        String location = authorize(authParams(clientId, verifier));
        String requestId = requestId(location);

        // 위임장: 로그인해야 보이고, 앱 이름·돌아갈 호스트를 보인다
        mvc.perform(get("/api/v1/oauth/requests/{id}", requestId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/oauth/requests/{id}", requestId).session(mySession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Claude"))
                .andExpect(jsonPath("$.redirectHost").value("claude.ai"))
                .andExpect(jsonPath("$.scope").doesNotExist())
                .andExpect(jsonPath("$.expiresAt").isString());

        String redirectTo = approve(mySession, requestId, "read");
        assertThat(redirectTo).startsWith(CALLBACK + "?code=");
        Map<String, String> back = query(redirectTo);
        assertThat(back.get("state")).isEqualTo("상태 값+1");
        assertThat(back.get("iss")).isEqualTo(ISSUER);
        // 요청은 한 번만 허락된다
        mvc.perform(post("/api/v1/oauth/requests/{id}/approve", requestId).with(csrf()).session(mySession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("scope", "read")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("OAUTH_REQUEST_NOT_FOUND"));

        MvcResult tokenResult = token("grant_type", "authorization_code", "code", back.get("code"), "client_id",
                clientId, "redirect_uri", CALLBACK, "code_verifier", verifier, "resource", RESOURCE)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("read")).andReturn();
        String access = JsonPath.read(tokenResult.getResponse().getContentAsString(), "$.access_token");
        assertThat(access).startsWith("go_");
        assertThat((String) JsonPath.read(tokenResult.getResponse().getContentAsString(), "$.refresh_token"))
                .startsWith("gr_");
        MvcResult tools = mcp(access, "tools/list");
        assertThat(tools.getResponse().getStatus()).isEqualTo(200);
        // S1-7 읽기만 허락한 연결에는 임시 저장 도구가 없다
        List<String> names = JsonPath.read(tools.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.result.tools[*].name");
        assertThat(names).contains("search_passages").doesNotContain("save_draft");
        // 출입증·갱신 토큰 원문은 저장하지 않는다
        assertThat(jdbc.queryForObject("select count(*) from oauth_access_token where token_hash = ?", Long.class,
                access)).isZero();
    }

    @Test
    void S1_7_읽기_더하기_초안으로_허락하면_임시_저장_도구가_보이고_앱이_읽기만_요청했으면_더_넓게_줄_수_없다() throws Exception {
        Connected draft = connect(mySession, "draft");
        List<String> names = JsonPath.read(mcp(draft.access(), "tools/list").getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.result.tools[*].name");
        assertThat(names).contains("save_draft");

        String clientId = newClient();
        Map<String, String> params = authParams(clientId, verifier());
        params.put("scope", "read");
        String requestId = requestId(authorize(params));
        mvc.perform(get("/api/v1/oauth/requests/{id}", requestId).session(mySession))
                .andExpect(jsonPath("$.scope").value("read"));
        mvc.perform(post("/api/v1/oauth/requests/{id}/approve", requestId).with(csrf()).session(mySession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("scope", "draft")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OAUTH_SCOPE_INVALID"));
        mvc.perform(post("/api/v1/oauth/requests/{id}/approve", requestId).with(csrf()).session(mySession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("scope", "everything")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void S1_3_모르는_앱과_등록하지_않은_주소는_돌려보내지_않고_그_밖의_잘못은_앱으로_알린다() throws Exception {
        String clientId = newClient("http://127.0.0.1:4000/cb");
        String verifier = verifier();
        Map<String, String> unknown = authParams("gc_nope", verifier);
        assertThat(authorize(unknown)).isEqualTo("/connect/authorize?error=invalid_client");
        Map<String, String> wrongRedirect = authParams(clientId, verifier);
        wrongRedirect.put("redirect_uri", "https://evil.example/cb");
        assertThat(authorize(wrongRedirect)).isEqualTo("/connect/authorize?error=invalid_client");

        // 루프백은 포트만 달라도 된다 (RFC 8252)
        Map<String, String> otherPort = authParams(clientId, verifier);
        otherPort.put("redirect_uri", "http://127.0.0.1:51234/cb");
        assertThat(authorize(otherPort)).startsWith("/connect/authorize?request=");

        // 그 밖의 잘못은 앱으로 바로 튕기지 않고 글결 오류 화면으로: 돌아갈 주소는 서버가 만들어 번호로만 준다
        Map<String, String> noPkce = authParams(clientId, verifier);
        noPkce.put("redirect_uri", "http://127.0.0.1:4000/cb");
        noPkce.remove("code_challenge");
        String errorPage = authorize(noPkce);
        assertThat(errorPage).startsWith("/connect/authorize?error=invalid_request&back=");
        String errorId = errorPage.substring(errorPage.indexOf("back=") + 5);
        String view = mvc.perform(get("/api/v1/oauth/errors/{id}", errorId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Claude"))
                .andExpect(jsonPath("$.redirectHost").value("127.0.0.1"))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String redirectTo = JsonPath.read(view, "$.redirectTo");
        assertThat(redirectTo).startsWith("http://127.0.0.1:4000/cb?");
        assertThat(query(redirectTo)).containsEntry("error", "invalid_request").containsEntry("state", "상태 값+1")
                .containsEntry("iss", ISSUER);
        mvc.perform(get("/api/v1/oauth/errors/{id}", "y".repeat(32))).andExpect(status().isNotFound());

        Map<String, String> plain = authParams(clientId, verifier);
        plain.put("redirect_uri", "http://127.0.0.1:4000/cb");
        plain.put("code_challenge_method", "plain");
        assertThat(authorize(plain)).startsWith("/connect/authorize?error=invalid_request&back=");
        Map<String, String> implicit = authParams(clientId, verifier);
        implicit.put("redirect_uri", "http://127.0.0.1:4000/cb");
        implicit.put("response_type", "token");
        assertThat(authorize(implicit)).startsWith("/connect/authorize?error=unsupported_response_type&back=");
        Map<String, String> badScope = authParams(clientId, verifier);
        badScope.put("redirect_uri", "http://127.0.0.1:4000/cb");
        badScope.put("scope", "read admin");
        assertThat(authorize(badScope)).startsWith("/connect/authorize?error=invalid_scope&back=");
        // S1-6 다른 자원을 달라고 하면 invalid_target
        Map<String, String> otherResource = authParams(clientId, verifier);
        otherResource.put("redirect_uri", "http://127.0.0.1:4000/cb");
        otherResource.put("resource", "https://other.example/mcp");
        assertThat(authorize(otherResource)).startsWith("/connect/authorize?error=invalid_target&back=");
    }

    @Test
    void 거절하면_앱에_access_denied를_알리고_모르는_요청은_404다() throws Exception {
        String clientId = newClient();
        String requestId = requestId(authorize(authParams(clientId, verifier())));
        String body = mvc.perform(post("/api/v1/oauth/requests/{id}/deny", requestId).with(csrf()).session(mySession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, String> back = query(JsonPath.read(body, "$.redirectTo"));
        assertThat(back).containsEntry("error", "access_denied").containsEntry("state", "상태 값+1");
        mvc.perform(get("/api/v1/oauth/requests/{id}", requestId).session(mySession)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/oauth/requests/{id}", "x".repeat(32)).session(mySession))
                .andExpect(status().isNotFound());
        // CSRF 없이 허락할 수 없다
        mvc.perform(post("/api/v1/oauth/requests/{id}/approve", requestId).session(mySession)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("scope", "read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void S1_4_허락_코드는_그_앱_그_주소_맞는_verifier로_한_번만이고_두_번_쓰면_연결을_끊는다() throws Exception {
        String clientId = newClient();
        String otherClient = newClient();
        String verifier = verifier();
        Map<String, String> back = query(approve(mySession, requestId(authorize(authParams(clientId, verifier))),
                "read"));
        String code = back.get("code");
        token("grant_type", "authorization_code", "code", code, "client_id", otherClient, "redirect_uri", CALLBACK,
                "code_verifier", verifier).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        // 위에서 코드를 이미 꺼냈다: 다른 앱이 가로채려 하면 코드는 사라진다
        token("grant_type", "authorization_code", "code", code, "client_id", clientId, "redirect_uri", CALLBACK,
                "code_verifier", verifier).andExpect(jsonPath("$.error").value("invalid_grant"));

        Map<String, String> again = query(approve(mySession, requestId(authorize(authParams(clientId, verifier))),
                "read"));
        token("grant_type", "authorization_code", "code", again.get("code"), "client_id", clientId, "redirect_uri",
                CALLBACK, "code_verifier", verifier()).andExpect(jsonPath("$.error").value("invalid_grant"));

        Connected connected = connect(mySession, "read");
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(200);
        token("grant_type", "authorization_code", "code", connected.code(), "client_id", connected.clientId(),
                "redirect_uri", CALLBACK, "code_verifier", connected.verifier()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(401);

        token("grant_type", "password", "username", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
        token("grant_type", "authorization_code").andExpect(jsonPath("$.error").value("invalid_request"));
        token("grant_type", "authorization_code", "code", "x", "client_id", "gc_unknown", "code_verifier", verifier)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("invalid_client"));
        mvc.perform(post("/oauth/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code", "refresh_token"))
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void S1_6_토큰_요청의_resource가_글결_MCP가_아니면_invalid_target() throws Exception {
        String clientId = newClient();
        String verifier = verifier();
        Map<String, String> back = query(approve(mySession, requestId(authorize(authParams(clientId, verifier))),
                "read"));
        token("grant_type", "authorization_code", "code", back.get("code"), "client_id", clientId, "redirect_uri",
                CALLBACK, "code_verifier", verifier, "resource", "https://other.example/mcp")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_target"));
    }

    @Test
    void S1_5_갱신_토큰은_쓸_때마다_바뀌고_유예가_지나_옛_갱신_토큰이_다시_오면_연결을_끊는다() throws Exception {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS); // DB는 마이크로초로 반올림해 저장한다
        clock.set(start);
        Connected connected = connect(mySession, "read");
        String body = token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id",
                connected.clientId()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newAccess = JsonPath.read(body, "$.access_token");
        String newRefresh = JsonPath.read(body, "$.refresh_token");
        assertThat(newRefresh).isNotEqualTo(connected.refresh());
        assertThat(mcp(newAccess, "ping").getResponse().getStatus()).isEqualTo(200);
        // 읽기만 허락한 연결에 초안까지 달라고 하면 거절하고, 갱신 토큰은 그대로 쓸 수 있다
        token("grant_type", "refresh_token", "refresh_token", newRefresh, "client_id", connected.clientId(), "scope",
                "draft").andExpect(jsonPath("$.error").value("invalid_scope"));
        // 다른 앱이 갱신하려 하면 안 된다
        token("grant_type", "refresh_token", "refresh_token", newRefresh, "client_id", newClient())
                .andExpect(jsonPath("$.error").value("invalid_grant"));

        // 바뀐 지 60초(유예)가 지나 옛 갱신 토큰이 오면 도난으로 보고 연결을 끊는다
        clock.set(start.plusSeconds(60));
        token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id", connected.clientId())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(newAccess, "ping").getResponse().getStatus()).isEqualTo(401);
        token("grant_type", "refresh_token", "refresh_token", newRefresh, "client_id", connected.clientId())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void S1_5_바뀐_지_유예_안에_옛_갱신_토큰이_오면_거절만_하고_연결은_그대로다() throws Exception {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS); // DB는 마이크로초로 반올림해 저장한다
        clock.set(start);
        Connected connected = connect(mySession, "read");
        String body = token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id",
                connected.clientId()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newAccess = JsonPath.read(body, "$.access_token");
        String newRefresh = JsonPath.read(body, "$.refresh_token");
        clock.set(start.plusSeconds(59));
        token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id", connected.clientId())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(newAccess, "ping").getResponse().getStatus()).isEqualTo(200);
        // 다른 앱이 옛 갱신 토큰을 보내도 그 연결을 끊지 않는다
        clock.set(start.plusSeconds(120));
        token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id", newClient())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(newAccess, "ping").getResponse().getStatus()).isEqualTo(200);
        token("grant_type", "refresh_token", "refresh_token", newRefresh, "client_id", connected.clientId())
                .andExpect(status().isOk());
    }

    @Test
    void S1_5_두_번_넘게_바뀐_옛_갱신_토큰이_오면_유예_안이어도_바로_연결을_끊는다() throws Exception {
        // 훔친 T1으로 T2·T3까지 돌렸어도, 피해자가 T1을 보내면 알아챈다 (바로 전 것만 보면 놓친다)
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        clock.set(start);
        Connected connected = connect(mySession, "read");
        String t1 = connected.refresh();
        String t2 = JsonPath.read(token("grant_type", "refresh_token", "refresh_token", t1, "client_id",
                connected.clientId()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.refresh_token");
        String body = token("grant_type", "refresh_token", "refresh_token", t2, "client_id", connected.clientId())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String t3 = JsonPath.read(body, "$.refresh_token");
        String access = JsonPath.read(body, "$.access_token");

        // 바로 전 것(T2)은 유예 안이면 거절만 한다
        clock.set(start.plusSeconds(10));
        token("grant_type", "refresh_token", "refresh_token", t2, "client_id", connected.clientId())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(access, "ping").getResponse().getStatus()).isEqualTo(200);
        // 다른 앱이 보낸 T1으로는 끊지 않는다
        token("grant_type", "refresh_token", "refresh_token", t1, "client_id", newClient())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(access, "ping").getResponse().getStatus()).isEqualTo(200);
        // 같은 앱이 그보다 옛것(T1)을 보내면 유예 안이어도 끊는다
        token("grant_type", "refresh_token", "refresh_token", t1, "client_id", connected.clientId())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(access, "ping").getResponse().getStatus()).isEqualTo(401);
        token("grant_type", "refresh_token", "refresh_token", t3, "client_id", connected.clientId())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(jdbc.queryForObject("select count(*) from oauth_grant where client_id = ?", Integer.class,
                connected.clientId())).isZero();
    }

    @Test
    void S1_5_같은_갱신_토큰으로_거의_동시에_두_번_갱신해도_하나만_받고_연결은_그대로다() throws Exception {
        Connected connected = connect(mySession, "read");
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        List<java.util.concurrent.Future<MvcResult>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            results.add(pool.submit(() -> {
                ready.await();
                return token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id",
                        connected.clientId()).andReturn();
            }));
        }
        ready.countDown();
        List<Integer> statuses = new ArrayList<>();
        String issuedAccess = null;
        for (var future : results) {
            MvcResult result = future.get();
            statuses.add(result.getResponse().getStatus());
            if (result.getResponse().getStatus() == 200) {
                issuedAccess = JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
            }
        }
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(200, 400);
        assertThat(jdbc.queryForObject("select count(*) from oauth_grant where users_id = ?", Long.class, me.id()))
                .isEqualTo(1);
        assertThat(mcp(issuedAccess, "ping").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void S1_4_다른_앱이_이미_쓴_코드를_보내도_그_연결은_끊지_않는다() throws Exception {
        Connected connected = connect(mySession, "read");
        token("grant_type", "authorization_code", "code", connected.code(), "client_id", newClient(), "redirect_uri",
                CALLBACK, "code_verifier", connected.verifier()).andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void S2_3_허락한_뒤_연결을_모두_끊었으면_그_전에_받은_허락_코드는_쓸_수_없다() throws Exception {
        String clientId = newClient();
        String verifier = verifier();
        Map<String, String> back = query(approve(mySession, requestId(authorize(authParams(clientId, verifier))),
                "read"));
        accessKeys.revokeAll(me.id());
        token("grant_type", "authorization_code", "code", back.get("code"), "client_id", clientId, "redirect_uri",
                CALLBACK, "code_verifier", verifier).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        assertThat(jdbc.queryForObject("select count(*) from oauth_grant where users_id = ?", Long.class, me.id()))
                .isZero();
        // 끊은 뒤에 새로 허락하면 다시 잇는다
        connect(mySession, "read");
    }

    @Test
    void 토큰과_폐기는_주소_뒤에_값을_실으면_거절한다() throws Exception {
        mvc.perform(post("/oauth/token?code=leak&grant_type=authorization_code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("client_id", "gc_x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
        mvc.perform(post("/oauth/revoke?token=gr_leak").contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void 앱_이름의_보이지_않는_서식_글자와_제어_글자는_뺀다() throws Exception {
        registerClient(randomIp(), "client_name", "\u202EClaude\u200B\u0007 공식", "redirect_uris", List.of(CALLBACK))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.client_name").value("Claude 공식"))
                .andDo(result -> clients.add(JsonPath.read(result.getResponse().getContentAsString(), "$.client_id")));
        registerClient(randomIp(), "client_name", "\u200B\u200E", "redirect_uris", List.of(CALLBACK))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.client_name").value("이름 없는 앱"))
                .andDo(result -> clients.add(JsonPath.read(result.getResponse().getContentAsString(), "$.client_id")));
    }

    @Test
    void 연결_없이_오래_쓰지_않은_앱_등록만_지운다() throws Exception {
        String stale = "gc_stale" + UUID.randomUUID().toString().substring(0, 8);
        String fresh = "gc_fresh" + UUID.randomUUID().toString().substring(0, 8);
        clients.add(stale);
        clients.add(fresh);
        jdbc.update("insert into oauth_client (client_id, client_name, redirect_uris, created_at, last_used_at)"
                + " values (?, 'a', 'https://a.example/cb', now() - interval '90 days', now() - interval '31 days')", stale);
        jdbc.update("insert into oauth_client (client_id, client_name, redirect_uris, created_at, last_used_at)"
                + " values (?, 'b', 'https://b.example/cb', now() - interval '90 days', now() - interval '1 day')", fresh);
        Connected connected = connect(mySession, "read");
        jdbc.update("update oauth_client set created_at = now() - interval '90 days',"
                + " last_used_at = now() - interval '90 days' where client_id = ?", connected.clientId());
        oauthService.cleanup();
        assertThat(jdbc.queryForList("select client_id from oauth_client where client_id in (?, ?, ?)", String.class,
                stale, fresh, connected.clientId())).containsExactlyInAnyOrder(fresh, connected.clientId());
    }

    @Test
    void 폐기는_갱신_토큰이면_연결을_출입증이면_그_출입증만_끊고_모르는_토큰도_200() throws Exception {
        Connected first = connect(mySession, "read");
        mvc.perform(post("/oauth/revoke").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", first.access()).param("client_id", first.clientId())).andExpect(status().isOk());
        assertThat(mcp(first.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        token("grant_type", "refresh_token", "refresh_token", first.refresh(), "client_id", first.clientId())
                .andExpect(status().isOk());

        Connected second = connect(mySession, "read");
        mvc.perform(post("/oauth/revoke").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", second.refresh())).andExpect(status().isOk());
        assertThat(mcp(second.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        mvc.perform(post("/oauth/revoke").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "gr_unknown")).andExpect(status().isOk());
    }

    @Test
    void S2_1_연결된_앱을_보고_끊으면_출입증이_막히고_남의_연결은_404() throws Exception {
        Connected connected = connect(mySession, "draft");
        String list = mvc.perform(get("/api/v1/me/connected-apps").session(mySession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.max").value(3))
                .andExpect(jsonPath("$.items[0].clientName").value("Claude"))
                .andExpect(jsonPath("$.items[0].scope").value("draft"))
                .andExpect(jsonPath("$.items[0].lastUsedAt").isString())
                .andReturn().getResponse().getContentAsString();
        Integer grantId = JsonPath.read(list, "$.items[0].grantId");

        Member other = register("ob");
        MockHttpSession otherSession = members.login(mvc, other);
        mvc.perform(delete("/api/v1/me/connected-apps/{id}", grantId).with(csrf()).session(otherSession))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("OAUTH_GRANT_NOT_FOUND"));
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(200);

        mvc.perform(delete("/api/v1/me/connected-apps/{id}", grantId).with(csrf()).session(mySession))
                .andExpect(status().isNoContent());
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id", connected.clientId())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        mvc.perform(get("/api/v1/me/connected-apps").session(mySession)).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/v1/me/connected-apps")).andExpect(status().isUnauthorized());
    }

    @Test
    void S2_2_연결이_한도를_넘으면_가장_오래_쓰지_않은_연결을_끊는다() throws Exception {
        Connected oldest = connect(mySession, "read");
        jdbc.update("update oauth_grant set last_used_at = now() - interval '3 days' where users_id = ?", me.id());
        Connected second = connect(mySession, "read");
        Connected third = connect(mySession, "read");
        Connected fourth = connect(mySession, "read");
        assertThat(jdbc.queryForObject("select count(*) from oauth_grant where users_id = ?", Long.class, me.id()))
                .isEqualTo(3);
        assertThat(mcp(oldest.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        for (Connected kept : List.of(second, third, fourth)) {
            assertThat(mcp(kept.access(), "ping").getResponse().getStatus()).isEqualTo(200);
        }
    }

    @Test
    void S2_3_정지되면_출입증이_막히고_비밀번호_변경_정지_탈퇴는_연결을_모두_끊는다() throws Exception {
        Connected connected = connect(mySession, "read");
        jdbc.update("update users set suspended_until = now() + interval '1 day', suspend_reason = '광고'"
                + " where users_id = ?", me.id());
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        token("grant_type", "refresh_token", "refresh_token", connected.refresh(), "client_id", connected.clientId())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        jdbc.update("update users set suspended_until = null, suspend_reason = null where users_id = ?", me.id());

        Member other = register("oc");
        MockHttpSession otherSession = members.login(mvc, other);
        Connected again = connect(otherSession, "read");
        assertThat(mcp(again.access(), "ping").getResponse().getStatus()).isEqualTo(200);
        accessKeys.revokeAll(other.id());
        assertThat(jdbc.queryForObject("select count(*) from oauth_grant where users_id = ?", Long.class, other.id()))
                .isZero();
        assertThat(mcp(again.access(), "ping").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void 출입증_모양이_아니거나_지난_출입증은_401() throws Exception {
        Connected connected = connect(mySession, "read");
        jdbc.update("update oauth_access_token set expires_at = now() - interval '1 second'"
                + " where oauth_grant_id in (select oauth_grant_id from oauth_grant where users_id = ?)", me.id());
        assertThat(mcp(connected.access(), "ping").getResponse().getStatus()).isEqualTo(401);
        assertThat(mcp("go_" + UUID.randomUUID(), "ping").getResponse().getStatus()).isEqualTo(401);
    }
}
