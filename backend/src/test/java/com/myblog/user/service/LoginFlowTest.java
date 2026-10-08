package com.myblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.SessionLifetimeFilter;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 로그인·로그아웃·잠금 (specs/001 US2, US3, quickstart S-5 ~ S-7). 실제 PostgreSQL로 확인한다. */
@SpringBootTest
@Transactional
class LoginFlowTest {

    private static final String PASSWORD = "abcd123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private String email;
    private User member;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        email = "l" + id + "@example.com";
        member = users.saveAndFlush(User.create(email, passwordEncoder.encode(PASSWORD), "m" + id));
    }

    @Test
    void 로그인하면_내_정보를_보고_로그아웃하면_끝난다() throws Exception {
        MvcResult result = login(email.toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(member.getId()))
                .andExpect(jsonPath("$.member.nickname").value(member.getNickname()))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.nickname").value(member.getNickname()));

        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 이미_로그아웃했어도_로그아웃은_204() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void 로그인할_때_세션_ID가_바뀐다() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf()).session(before)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(oldId);
    }

    @Test
    void 이메일이_없든_비밀번호가_틀리든_같은_답() throws Exception {
        login(email, "wrong123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));
        login("nobody-" + email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"));
    }

    @Test
    void 비어_있으면_400() throws Exception {
        login("", "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 다섯_번_틀리면_잠기고_맞는_비밀번호도_거절된다() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login(email, "wrong123!").andExpect(status().isUnauthorized());
        }
        login(email, "wrong123!") // 5번째 응답이 잠금 문구 (US3-1)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value("로그인 시도가 5회 실패해 잠겼습니다. 10분 뒤에 다시 시도해 주세요"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        login(email, PASSWORD).andExpect(status().isLocked());
        assertThat(users.findFailedLoginCount(member.getId())).isEqualTo(5); // 잠긴 동안의 시도는 세지 않는다
    }

    @Test
    void 잠금_시각이_지나면_다시_로그인할_수_있고_횟수가_0이_된다() throws Exception {
        for (int i = 0; i < 5; i++) {
            users.incrementFailedLoginCount(member.getId());
        }
        users.lockUntil(member.getId(), Instant.now().minusSeconds(1));

        login(email, PASSWORD).andExpect(status().isOk());
        assertThat(users.findFailedLoginCount(member.getId())).isZero();
    }

    @Test
    void 성공하면_틀린_횟수가_0이_된다() throws Exception {
        login(email, "wrong123!").andExpect(status().isUnauthorized());
        login(email, "wrong123!").andExpect(status().isUnauthorized());
        assertThat(users.findFailedLoginCount(member.getId())).isEqualTo(2);

        login(email, PASSWORD).andExpect(status().isOk());
        assertThat(users.findFailedLoginCount(member.getId())).isZero();
    }

    @Test
    void 로그인하고_30일이_지나면_계속_써도_로그인이_끝난다() throws Exception {
        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        session.setAttribute(SessionLifetimeFilter.LOGIN_AT, Instant.now().minus(Duration.ofDays(30)).minusSeconds(1).toEpochMilli());

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void 쓸_때마다_세션_쿠키를_다시_내려_준다() throws Exception {
        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        MvcResult me = mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(me.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("SESSION=").contains("Max-Age=604800"));
    }

    private ResultActions login(String mailAddress, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + mailAddress + "\",\"password\":\"" + password + "\"}"));
    }
}
