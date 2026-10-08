package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.config.SessionConfig;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 비밀번호를 바꾸면 지금 기기만 로그인이 남고 다른 기기는 모두 끊긴다 (specs/002 T022, quickstart S-5, FR-020, SC-005).
 * MockMvc에 Spring Session 필터를 더해 실제 JDBC 세션(쿠키 SESSION)으로 확인한다. @Transactional은 쓰지 않는다.
 */
@SpringBootTest
class AccountSessionFlowTest {

    private static final String PASSWORD = "abcd123!";
    private static final String NEW_PASSWORD = "efgh456@";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private User member;

    @BeforeEach
    void setUp() {
        Filter sessionFilter = context.getBean("springSessionRepositoryFilter", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(sessionFilter).apply(springSecurity()).build();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        member = users.saveAndFlush(User.create("s" + id + "@example.com", passwordEncoder.encode(PASSWORD), "s" + id));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from spring_session where principal_name = ?", String.valueOf(member.getId()));
        jdbc.update("delete from users where users_id = ?", member.getId());
    }

    @Test
    void 비밀번호를_바꾼_기기만_로그인이_남는다() throws Exception {
        Cookie phone = login();
        Cookie laptop = login();
        Cookie pc = login();
        long createdBefore = creationTime(phone);

        mvc.perform(post("/api/account/password").with(csrf()).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD
                                + "\",\"newPasswordConfirm\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").cookie(phone)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(laptop)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(pc)).andExpect(status().isUnauthorized());
        assertThat(creationTime(phone)).isEqualTo(createdBefore); // 남은 세션은 새로 만든 것이 아니다
        assertThat(jdbc.queryForObject("select count(*) from spring_session where principal_name = ?", Integer.class,
                String.valueOf(member.getId()))).isEqualTo(1);
    }

    private Cookie login() throws Exception {
        Cookie cookie = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + member.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SessionConfig.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long creationTime(Cookie cookie) {
        String sessionId = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        return jdbc.queryForObject("select creation_time from spring_session where session_id = ?", Long.class, sessionId);
    }
}
