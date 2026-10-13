package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.service.MemberRegistration;
import com.myblog.user.social.SocialLoginHandlers;
import com.myblog.user.social.SocialOAuth2User;
import com.myblog.user.social.SocialProfile;
import com.myblog.user.social.SocialProvider;
import com.myblog.user.social.SocialSessionState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 소셜 로그인 (specs/010 US1 ~ US5). 회사 화면은 흉내 낸다: 회사 로그인을 마친 순간(성공 처리기)부터 실제 PostgreSQL로 확인한다.
 * GitHub만 열쇠를 넣어 "열쇠가 있는 회사만" 규칙(D-9)도 함께 본다.
 */
@SpringBootTest(properties = {
        "social.clients.github.client-id=test-github-id",
        "social.clients.github.client-secret=test-github-secret"})
class SocialLoginFlowTest {

    private static final String PASSWORD = "abcd123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SocialLoginHandlers handlers;

    @Autowired
    private MemberRegistration registration;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String id;
    private final List<String> nicknames = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @AfterEach
    void cleanUp() {
        for (String nickname : nicknames) {
            List<Long> ids = jdbc.queryForList("select users_id from users where lower(nickname) = lower(?)", Long.class, nickname);
            for (Long userId : ids) {
                jdbc.update("delete from user_social_account where users_id = ?", userId);
                jdbc.update("delete from category where blog_id in (select blog_id from blog where users_id = ?)", userId);
                jdbc.update("delete from blog where users_id = ?", userId);
                jdbc.update("delete from users where users_id = ?", userId);
            }
        }
        jdbc.update("delete from user_social_account where provider_user_id like ?", "t" + id + "%");
    }

    @Test
    void 열쇠가_있는_회사만_버튼이_보이고_회사_화면으로_보낸다() throws Exception {
        mvc.perform(get("/api/v1/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(1))
                .andExpect(jsonPath("$.providers[0]").value("github"));

        String location = mvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://github.com/login/oauth/authorize")
                .contains("client_id=test-github-id")
                .contains("redirect_uri=http://localhost/login/oauth2/code/github")
                .contains("scope=read:user%20user:email");

        // 열쇠가 없는 회사는 회사 화면으로 가지 않고 다른 주소처럼 401로 끝난다 (서버 오류 500이 아니다)
        mvc.perform(get("/oauth2/authorization/kakao")).andExpect(status().isUnauthorized());
    }

    @Test
    void 처음_사용자는_닉네임만_정하면_가입되고_바로_로그인된다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SocialProfile profile = profile("n" + id + "@example.com", true, "n" + id);

        assertThat(returnFromProvider(profile, session)).isEqualTo("/signup/social");
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/social/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("github"))
                .andExpect(jsonPath("$.email").value("n" + id + "@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.nickname").value("n" + id))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());

        nicknames.add("새" + id.substring(0, 6));
        signup(session, "{\"nickname\":\"새" + id.substring(0, 6) + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.member.nickname").value("새" + id.substring(0, 6)));

        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.nickname").value("새" + id.substring(0, 6)));
        Long memberId = memberIdOf("n" + id + "@example.com");
        assertThat(jdbc.queryForObject("select password from users where users_id = ?", String.class, memberId)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from blog where users_id = ?", Integer.class, memberId)).isOne();
        mvc.perform(get("/api/v1/auth/social/pending").session(session)).andExpect(status().isNotFound());

        // 다음부터는 회사 로그인만으로 들어온다
        MockHttpSession next = new MockHttpSession();
        assertThat(returnFromProvider(profile, next)).isEqualTo("/");
        mvc.perform(get("/api/v1/auth/me").session(next))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(memberId));

        // 비밀번호가 없으니 이메일 로그인은 안 된다
        loginRequest("n" + id + "@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void 회사가_인증한_이메일과_같은_회원은_자동으로_연결된다() throws Exception {
        User member = emailMember("a");
        MockHttpSession session = new MockHttpSession();

        assertThat(returnFromProvider(profile(member.getEmail().toUpperCase(), true, "x"), session)).isEqualTo("/");
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(member.getId()));
        mvc.perform(get("/api/v1/account/social").session(session))
                .andExpect(jsonPath("$.hasPassword").value(true))
                .andExpect(jsonPath("$.linked[0].provider").value("github"));
    }

    @Test
    void 인증되지_않은_이메일은_연결하지_않고_이메일_인증을_거쳐야_한다() throws Exception {
        User member = emailMember("b");
        MockHttpSession session = new MockHttpSession();

        assertThat(returnFromProvider(profile(member.getEmail(), false, null), session)).isEqualTo("/signup/social");
        assertThat(jdbc.queryForObject("select count(*) from user_social_account where users_id = ?", Integer.class,
                member.getId())).isZero();
        mvc.perform(get("/api/v1/auth/social/pending").session(session))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.nickname").doesNotExist());

        signup(session, "{\"nickname\":\"b2" + id.substring(0, 6) + "\",\"email\":\"" + member.getEmail()
                + "\",\"verificationToken\":\"nope\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 시간이_지난_가입_대기는_없는_것으로_본다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SocialSessionState.PENDING, new SocialSessionState.Pending(SocialProvider.GITHUB,
                "t" + id, "e" + id + "@example.com", true, null, Instant.now().minusSeconds(1)));

        mvc.perform(get("/api/v1/auth/social/pending").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOCIAL_PENDING_NOT_FOUND"));
        signup(session, "{\"nickname\":\"e" + id + "\"}").andExpect(status().isNotFound());
    }

    @Test
    void 마이페이지에서_연결하고_다른_회원의_계정은_연결할_수_없다() throws Exception {
        User member = emailMember("c");
        MockHttpSession session = login(member.getEmail());

        mvc.perform(post("/api/v1/account/social/kakao/link").with(csrf()).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOCIAL_PROVIDER_UNKNOWN"));
        mvc.perform(post("/api/v1/account/social/github/link").with(csrf()).session(session))
                .andExpect(status().isNoContent());
        SocialProfile profile = profile(null, false, null);
        assertThat(returnFromProvider(profile, session)).isEqualTo("/mypage?linked=github");
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(member.getId()));

        // 연결하기 표시가 시간이 지났으면 연결하지 않고, 로그인한 회원도 그대로다
        User late = emailMember("e");
        MockHttpSession lateSession = login(late.getEmail());
        lateSession.setAttribute(SocialSessionState.LINK,
                new SocialSessionState.LinkIntent(late.getId(), SocialProvider.GITHUB, Instant.now().minusSeconds(1)));
        assertThat(returnFromProvider(profile, lateSession)).isEqualTo("/mypage?linkError=SOCIAL_LOGIN_FAILED");
        mvc.perform(get("/api/v1/auth/me").session(lateSession))
                .andExpect(jsonPath("$.member.id").value(late.getId()));

        // 같은 GitHub 계정을 다른 회원이 연결하려 하면
        User other = emailMember("d");
        MockHttpSession otherSession = login(other.getEmail());
        mvc.perform(post("/api/v1/account/social/github/link").with(csrf()).session(otherSession))
                .andExpect(status().isNoContent());
        assertThat(returnFromProvider(profile, otherSession)).isEqualTo("/mypage?linkError=SOCIAL_ACCOUNT_IN_USE");
        mvc.perform(get("/api/v1/auth/me").session(otherSession))
                .andExpect(jsonPath("$.member.id").value(other.getId()));
    }

    @Test
    void 마지막_로그인_방법은_해제할_수_없고_비밀번호를_만들면_해제된다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        returnFromProvider(profile("f" + id + "@example.com", true, null), session);
        nicknames.add("f" + id);
        signup(session, "{\"nickname\":\"f" + id + "\"}").andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/account/social/github").with(csrf()).session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_LOGIN_METHOD"));

        // 비밀번호 만들기: 현재 비밀번호 없이
        mvc.perform(post("/api/v1/account/password").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"" + PASSWORD + "\",\"newPasswordConfirm\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/account/social").session(session)).andExpect(jsonPath("$.hasPassword").value(true));
        loginRequest("f" + id + "@example.com", PASSWORD).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/account/social/github").with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/account/social/github").with(csrf()).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOCIAL_NOT_LINKED"));
    }

    @Test
    void 비밀번호_없는_회원은_동의만으로_탈퇴하고_연결도_지워진다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SocialProfile profile = profile("g" + id + "@example.com", true, null);
        returnFromProvider(profile, session);
        nicknames.add("g" + id);
        signup(session, "{\"nickname\":\"g" + id + "\"}").andExpect(status().isCreated());
        Long memberId = memberIdOf("g" + id + "@example.com");

        mvc.perform(post("/api/v1/account/withdrawal").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from user_social_account where users_id = ?", Integer.class,
                memberId)).isZero();
        nicknames.add("탈퇴한사용자" + memberId);
        // 같은 계정으로 다시 오면 새 가입
        assertThat(returnFromProvider(profile, new MockHttpSession())).isEqualTo("/signup/social");
    }

    @Test
    void 비밀번호가_있는_회원은_탈퇴할_때_비밀번호가_필요하다() throws Exception {
        User member = emailMember("h");
        MockHttpSession session = login(member.getEmail());

        mvc.perform(post("/api/v1/account/withdrawal").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("PASSWORD_REQUIRED"));
    }

    /** 회사 화면에서 돌아온 순간: 성공 처리기를 그 세션으로 부르고, 보낸 화면 주소를 돌려준다. */
    private String returnFromProvider(SocialProfile profile, MockHttpSession session) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/github");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(new SocialOAuth2User(profile), List.of(), "github");
        handlers.onAuthenticationSuccess(request, response, token);
        return response.getRedirectedUrl();
    }

    private SocialProfile profile(String email, boolean verified, String name) {
        return new SocialProfile(SocialProvider.GITHUB, "t" + id + (email == null ? "" : email.hashCode()), email, verified, name);
    }

    private User emailMember(String prefix) {
        String nickname = prefix + id;
        nicknames.add(nickname);
        return registration.register(prefix + id + "@example.com", passwordEncoder.encode(PASSWORD), nickname);
    }

    private Long memberIdOf(String email) {
        return users.findActiveByEmail(email).orElseThrow().getId();
    }

    private ResultActions signup(MockHttpSession session, String body) throws Exception {
        return mvc.perform(post("/api/v1/auth/social/signup").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) loginRequest(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
