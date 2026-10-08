package com.myblog.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.service.MemberRegistration;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 마이페이지의 내 정보 보기·고치기 (specs/002 US1, quickstart S-1 ~ S-2, S-11의 1). 실제 PostgreSQL로 확인한다. */
@SpringBootTest
@Transactional
class AccountProfileTest {

    private static final String PASSWORD = "abcd123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private MemberRegistration registration;

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
        member = register("a");
        session = login(member.getEmail());
    }

    @Test
    void 내_정보를_본다_비밀번호는_없다() throws Exception {
        mvc.perform(get("/api/account").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(member.getEmail()))
                .andExpect(jsonPath("$.nickname").value(member.getNickname()))
                .andExpect(jsonPath("$.intro").value(""))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty())
                .andExpect(jsonPath("$.blog.id").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void 로그인하지_않으면_네_주소_모두_401() throws Exception {
        mvc.perform(get("/api/account")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(patch("/api/account/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/withdrawal").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 저장하면_닉네임과_소개가_바뀌고_머리글_닉네임도_바뀐다() throws Exception {
        String nickname = "새" + suffix().substring(0, 6);
        save("{\"nickname\":\"" + nickname + "\",\"intro\":\"안녕하세요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("저장했습니다"))
                .andExpect(jsonPath("$.nickname").value(nickname))
                .andExpect(jsonPath("$.intro").value("안녕하세요"));

        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.member.nickname").value(nickname));
        mvc.perform(get("/api/account").session(session)).andExpect(jsonPath("$.intro").value("안녕하세요"));
    }

    @Test
    void 이메일과_남의_회원_번호를_넣어도_내_닉네임과_소개만_바뀐다() throws Exception {
        User other = register("b");
        String nickname = "나" + suffix().substring(0, 6);
        save("{\"nickname\":\"" + nickname + "\",\"intro\":\"\",\"email\":\"hacker@example.com\",\"userId\":"
                + other.getId() + ",\"id\":" + other.getId() + "}")
                .andExpect(status().isOk());

        assertThat(users.findById(member.getId()).orElseThrow().getEmail()).isEqualTo(member.getEmail());
        assertThat(users.findById(other.getId()).orElseThrow().getNickname()).isEqualTo(other.getNickname());
    }

    @Test
    void 내_닉네임_그대로와_대소문자만_바꾸는_것은_된다() throws Exception {
        save("{\"nickname\":\"" + member.getNickname() + "\",\"intro\":\"\"}").andExpect(status().isOk());
        save("{\"nickname\":\"" + member.getNickname().toUpperCase() + "\",\"intro\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(member.getNickname().toUpperCase()));
    }

    @Test
    void 남의_닉네임은_대소문자가_달라도_409() throws Exception {
        User other = register("c");
        save("{\"nickname\":\"" + other.getNickname().toUpperCase() + "\",\"intro\":\"\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_USED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("이미 사용 중인 닉네임입니다"));
    }

    @Test
    void 닉네임_규칙을_어기면_400() throws Exception {
        for (String bad : new String[] {"가", "가나다라마바사아자차카", "철수!", "", " "}) {
            save("{\"nickname\":\"" + bad + "\",\"intro\":\"\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"));
        }
        save("{\"intro\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void 소개는_100자까지_되고_101자는_안_된다_이모지는_한_글자() throws Exception {
        save(profile("가".repeat(100))).andExpect(status().isOk());
        save(profile("😀".repeat(100))).andExpect(status().isOk());
        save(profile("가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("intro"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INTRO_TOO_LONG"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 소개는 100자 이하로 입력해 주세요"));
    }

    @Test
    void 소개를_비우면_비어_있는_것으로_저장된다() throws Exception {
        save(profile("잠깐")).andExpect(status().isOk());
        save(profile("")).andExpect(status().isOk()).andExpect(jsonPath("$.intro").value(""));
        assertThat(users.findById(member.getId()).orElseThrow().getIntro()).isNull();
    }

    @Test
    void 소개의_HTML은_글자_그대로_저장한다() throws Exception {
        save(profile("<script>alert(1)</script>")).andExpect(status().isOk())
                .andExpect(jsonPath("$.intro").value("<script>alert(1)</script>"));
    }

    @Test
    void CSRF_토큰이_없으면_거절한다() throws Exception {
        mvc.perform(patch("/api/account/profile").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(profile("x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 탈퇴한_회원의_세션은_401() throws Exception {
        jdbc.update("update users set deleted_at = now() where users_id = ?", member.getId());

        mvc.perform(get("/api/account").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    private String profile(String intro) {
        return "{\"nickname\":\"" + member.getNickname() + "\",\"intro\":\"" + intro + "\"}";
    }

    private ResultActions save(String body) throws Exception {
        return mvc.perform(patch("/api/account/profile").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User register(String prefix) {
        String id = suffix();
        return registration.register(prefix + id + "@example.com", passwordEncoder.encode(PASSWORD), prefix + id.substring(0, 7));
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
