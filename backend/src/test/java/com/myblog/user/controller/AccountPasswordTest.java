package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 비밀번호 변경 (specs/002 US2, quickstart S-4, S-6). 실제 PostgreSQL로 확인한다.
 * 클래스 전체에 @Transactional을 걸지 않는다: 걸면 "틀린 횟수 +1이 오류 응답과 함께 되돌려지는" 실수를 잡지 못한다.
 */
@SpringBootTest
class AccountPasswordTest {

    private static final String PASSWORD = "abcd123!";
    private static final String NEW_PASSWORD = "efgh456@";
    private static final String WRONG = "wrong123!";

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
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        member = users.saveAndFlush(User.create("p" + id + "@example.com", passwordEncoder.encode(PASSWORD), "p" + id));
        session = login(member.getEmail(), PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from users where users_id = ?", member.getId());
    }

    @Test
    void 바꾸면_옛_비밀번호로는_안_되고_새_비밀번호로_로그인된다() throws Exception {
        change(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호를 변경했습니다"));

        loginRequest(member.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        loginRequest(member.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 현재_비밀번호가_틀리면_400이고_로그인_창을_띄우는_401이_아니다() throws Exception {
        String hashBefore = hash();
        change(WRONG, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));

        assertThat(hash()).isEqualTo(hashBefore);
        assertThat(failedCount()).isEqualTo(1);
    }

    @Test
    void 새_비밀번호가_현재와_같으면_거절한다() throws Exception {
        change(PASSWORD, PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_SAME_AS_CURRENT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
    }

    @Test
    void 형식이_틀리면_칸별로_알리고_실패_횟수에_넣지_않는다() throws Exception {
        String hashBefore = hash();
        for (String bad : new String[] {"ab1!", "abcdefgh1!abcdefgh1!x", "abcdefgh!", "abcd1234", "abcd 123!", "가나다라123!"}) {
            change(PASSWORD, bad, bad)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
        }
        change(PASSWORD, NEW_PASSWORD, NEW_PASSWORD + "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPasswordConfirm"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("비밀번호가 일치하지 않습니다"));
        change("", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 현재 비밀번호를 입력해 주세요"));
        change(WRONG, "", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(hash()).isEqualTo(hashBefore);
        assertThat(failedCount()).isZero();
    }

    @Test
    void 로그인_실패와_합쳐_5번째에_잠기고_잠긴_동안은_맞아도_거절한다() throws Exception {
        for (int i = 0; i < 3; i++) {
            loginRequest(member.getEmail(), WRONG).andExpect(status().isUnauthorized());
        }
        change(WRONG, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        change(WRONG, NEW_PASSWORD, NEW_PASSWORD) // 5번째 응답이 잠금 (D-3 문구)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value("비밀번호를 5회 잘못 입력해 잠겼습니다. 10분 뒤에 다시 시도해 주세요"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        change(PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isLocked());
        loginRequest(member.getEmail(), PASSWORD).andExpect(status().isLocked());

        // 잠금 시간이 지나면 된다
        jdbc.update("update users set locked_until = now() - interval '1 second' where users_id = ?", member.getId());
        change(PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        assertThat(failedCount()).isZero();
    }

    @Test
    void 현재_비밀번호가_맞으면_틀린_횟수를_0으로_되돌린다() throws Exception {
        change(WRONG, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        change(WRONG, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());
        assertThat(failedCount()).isEqualTo(2);

        change(PASSWORD, PASSWORD, PASSWORD).andExpect(status().isBadRequest()); // 같은 값이라 거절되지만 현재 비밀번호는 맞다
        assertThat(failedCount()).isZero();
    }

    @Test
    void CSRF_토큰이_없으면_거절한다() throws Exception {
        mvc.perform(post("/api/account/password").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private ResultActions change(String current, String newPassword, String confirm) throws Exception {
        return mvc.perform(post("/api/account/password").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body(current, newPassword, confirm)));
    }

    private static String body(String current, String newPassword, String confirm) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + newPassword
                + "\",\"newPasswordConfirm\":\"" + confirm + "\"}";
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) loginRequest(email, password).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private String hash() {
        return jdbc.queryForObject("select password from users where users_id = ?", String.class, member.getId());
    }

    private int failedCount() {
        return jdbc.queryForObject("select failed_login_count from users where users_id = ?", Integer.class, member.getId());
    }
}
