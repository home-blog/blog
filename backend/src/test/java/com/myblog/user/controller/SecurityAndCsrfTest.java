package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 공통 바탕 확인 (specs/001 Phase 2 Checkpoint):
 * 서버가 뜨고, GET /api/auth/csrf가 토큰을 주며, 로그인·CSRF 규칙이 공통 오류 모양으로 응답한다.
 * 실제 CSRF 저장소(쿠키)를 확인하므로 따로 설정을 띄운다. 다른 테스트의 csrf() 도구는 같은 설정 안의
 * CSRF 저장소를 테스트용으로 바꿔 버리기 때문이다.
 */
@SpringBootTest
@TestPropertySource(properties = "myblog.test.context=real-csrf")
class SecurityAndCsrfTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthProperties authProperties;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 설정값_13개를_읽는다() {
        assertThat(authProperties.nickname().minLength()).isEqualTo(2);
        assertThat(authProperties.nickname().maxLength()).isEqualTo(10);
        assertThat(authProperties.password().minLength()).isEqualTo(8);
        assertThat(authProperties.password().maxLength()).isEqualTo(20);
        assertThat(authProperties.password().allowedSpecials()).isEqualTo("!@#$%^&*()_+-=");
        assertThat(authProperties.emailVerification().codeLength()).isEqualTo(6);
        assertThat(authProperties.emailVerification().codeTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(authProperties.emailVerification().resendInterval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(authProperties.emailVerification().dailyLimit()).isEqualTo(5);
        assertThat(authProperties.emailVerification().maxWrongAttempts()).isEqualTo(5);
        assertThat(authProperties.emailVerification().verifiedTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(authProperties.login().maxFailedAttempts()).isEqualTo(5);
        assertThat(authProperties.login().lockDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(authProperties.session().idleTimeout()).isEqualTo(Duration.ofDays(7));
        assertThat(authProperties.session().absoluteTimeout()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void CSRF_토큰을_준다() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    void 로그인이_필요한_요청은_401_UNAUTHENTICATED() throws Exception {
        mvc.perform(get("/api/members/me/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("※ 로그인이 필요합니다"));
    }

    @Test
    void CSRF_토큰_없이_변경_요청을_보내면_403() throws Exception {
        mvc.perform(post("/api/auth/csrf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void 받은_CSRF_토큰을_실으면_CSRF_검사를_통과한다() throws Exception {
        MvcResult issued = mvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie csrfCookie = issued.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        String body = issued.getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // CSRF 검사는 통과하고, 이 주소에 POST가 없어서 405가 나온다 (공통 오류 모양)
        mvc.perform(post("/api/auth/csrf").cookie(csrfCookie).header("X-XSRF-TOKEN", token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 서버_상태_확인은_로그인_없이_된다() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
