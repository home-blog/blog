package com.myblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.user.config.SessionConfig;
import com.myblog.user.domain.User;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 회원 탈퇴 (specs/002 US3, T029, quickstart S-7 ~ S-9a). 실제 PostgreSQL과 JDBC 세션으로 확인한다.
 * 클래스 전체에 @Transactional을 걸지 않는다 (틀린 횟수 +1, 탈퇴 취소를 그대로 봐야 해서). 끝에서 지운다.
 */
@SpringBootTest
class WithdrawalFlowTest {

    private static final String PASSWORD = "abcd123!";

    /** 켜면 블로그를 닫는 중에 아래 모듈이 실패한 것처럼 한다 (S-8의 7: 전부 취소되는지). */
    static final AtomicBoolean FAIL_ON_CLOSE = new AtomicBoolean(false);

    @TestConfiguration
    static class FailingListener {
        @Bean
        Object failOnBlogClosing() {
            return new Object() {
                @EventListener
                public void on(BlogClosingEvent event) {
                    if (FAIL_ON_CLOSE.get()) {
                        throw new IllegalStateException("아래 모듈 정리 실패 (테스트)");
                    }
                }
            };
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRegistration registration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private User member;
    private final List<Long> memberIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Filter sessionFilter = context.getBean("springSessionRepositoryFilter", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(sessionFilter).apply(springSecurity()).build();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        member = register("w" + id + "@example.com", "w" + id);
    }

    @AfterEach
    void cleanUp() {
        FAIL_ON_CLOSE.set(false);
        for (Long id : memberIds) {
            jdbc.update("delete from spring_session where principal_name = ?", String.valueOf(id));
            jdbc.update("delete from category where blog_id in (select blog_id from blog where users_id = ?)", id);
            jdbc.update("delete from blog where users_id = ?", id);
            jdbc.update("delete from users where users_id = ?", id);
        }
    }

    @Test
    void 탈퇴하면_블로그와_분류가_지워지고_회원_줄은_알아볼_수_없게_남는다() throws Exception {
        Cookie here = login(member.getEmail());
        Cookie other = login(member.getEmail());

        withdraw(here, PASSWORD, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("탈퇴가 완료되었습니다"));

        Map<String, Object> row = jdbc.queryForMap(
                "select email, password, nickname, intro, deleted_at from users where users_id = ?", member.getId());
        assertThat(row.get("email")).isEqualTo("deleted-" + member.getId() + "@deleted.invalid");
        assertThat(row.get("password")).isEqualTo("!deleted");
        assertThat(row.get("nickname")).isEqualTo("탈퇴한사용자" + member.getId());
        assertThat(row.get("intro")).isNull();
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(blogCount(member.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from category c where not exists "
                + "(select 1 from blog b where b.blog_id = c.blog_id)", Integer.class)).isZero();

        // 모든 기기에서 로그아웃 (FR-027), 옛 이메일로는 로그인할 수 없다
        mvc.perform(get("/api/auth/me").cookie(here)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(other)).andExpect(status().isUnauthorized());
        loginRequest(member.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호가_틀리면_아무것도_지우지_않고_틀린_횟수만_오른다() throws Exception {
        Cookie here = login(member.getEmail());

        withdraw(here, "wrong123!", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));

        assertThat(blogCount(member.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select failed_login_count from users where users_id = ? and deleted_at is null",
                Integer.class, member.getId())).isEqualTo(1);
        mvc.perform(get("/api/auth/me").cookie(here)).andExpect(status().isOk());
    }

    @Test
    void 안내에_동의하지_않거나_비밀번호가_비면_400이고_횟수에_넣지_않는다() throws Exception {
        Cookie here = login(member.getEmail());

        withdraw(here, PASSWORD, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("agreed"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 탈퇴 안내를 확인해 주세요"));
        withdraw(here, "", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 비밀번호를 입력해 주세요"));
        mvc.perform(post("/api/account/withdrawal").with(csrf()).cookie(here).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(blogCount(member.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select failed_login_count from users where users_id = ?", Integer.class,
                member.getId())).isZero();
    }

    @Test
    void 아래_모듈_정리가_실패하면_탈퇴_전체가_취소된다() throws Exception {
        Cookie here = login(member.getEmail());
        FAIL_ON_CLOSE.set(true);

        withdraw(here, PASSWORD, true).andExpect(status().is5xxServerError());

        assertThat(blogCount(member.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from users where users_id = ? and deleted_at is null",
                Integer.class, member.getId())).isEqualTo(1);
        mvc.perform(get("/api/auth/me").cookie(here)).andExpect(status().isOk());
    }

    @Test
    void 탈퇴한_뒤_같은_이메일과_닉네임으로_다시_가입하면_새_회원과_새_블로그가_생긴다() throws Exception {
        withdraw(login(member.getEmail()), PASSWORD, true).andExpect(status().isOk());

        User again = register(member.getEmail(), member.getNickname());

        assertThat(again.getId()).isNotEqualTo(member.getId());
        assertThat(jdbc.queryForList("select c.name from category c join blog b on b.blog_id = c.blog_id where b.users_id = ?",
                String.class, again.getId())).containsExactly("미분류");
    }

    private User register(String email, String nickname) {
        User user = registration.register(email, passwordEncoder.encode(PASSWORD), nickname);
        memberIds.add(user.getId());
        return user;
    }

    private ResultActions withdraw(Cookie session, String password, boolean agreed) throws Exception {
        return mvc.perform(post("/api/account/withdrawal").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"" + password + "\",\"agreed\":" + agreed + "}"));
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private Cookie login(String email) throws Exception {
        return loginRequest(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SessionConfig.COOKIE_NAME);
    }

    private int blogCount(Long memberId) {
        return jdbc.queryForObject("select count(*) from blog where users_id = ?", Integer.class, memberId);
    }
}
