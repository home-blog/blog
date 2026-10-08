package com.myblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.user.domain.User;
import com.myblog.user.mail.MailSendFailedException;
import com.myblog.user.mail.VerificationMailSender;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.verification.EmailVerificationStore;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 이메일 인증 + 가입 (specs/001 US1, quickstart S-1 ~ S-3). 실제 PostgreSQL과 Redis로 확인한다.
 * 메일은 보낸 번호를 기억하는 테스트용 발송기로 바꿔 끼운다.
 */
@SpringBootTest
@Transactional
class SignupFlowTest {

    @TestConfiguration
    static class CapturingMailConfig {
        @Bean
        @Primary
        CapturingMailSender capturingMailSender() {
            return new CapturingMailSender();
        }
    }

    static class CapturingMailSender implements VerificationMailSender {
        final Map<String, String> lastCode = new ConcurrentHashMap<>();
        volatile boolean fail;

        @Override
        public void send(String email, String code, Duration validFor) {
            if (fail) {
                throw new MailSendFailedException("테스트용 발송 실패", null);
            }
            lastCode.put(email, code);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CapturingMailSender mail;

    @Autowired
    private UserRepository users;

    @Autowired
    private BlogRepository blogs;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private EmailVerificationStore store;

    private MockMvc mvc;
    private String email;
    private String nickname;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mail.fail = false;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        email = "u" + id + "@example.com";
        nickname = "n" + id;
    }

    @Test
    void 인증을_마치면_가입되고_블로그와_미분류가_함께_생긴다() throws Exception {
        String body = sendCode(nickname, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호를 보냈습니다. 10분 안에 입력해 주세요"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"verificationToken\":\"([^\"]+)\".*", "$1");

        confirm(email, mail.lastCode.get(email), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다"));

        signup(nickname, email, "abcd123!", "abcd123!", token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("가입이 완료되었습니다. 로그인해 주세요"));

        User member = users.findActiveByEmail(email).orElseThrow();
        assertThat(member.getNickname()).isEqualTo(nickname);
        assertThat(member.getPasswordHash()).isNotEqualTo("abcd123!").startsWith("$2");
        Blog blog = blogs.findByOwnerId(member.getId()).orElseThrow();
        assertThat(blog.getName()).isEqualTo(nickname + "의 블로그");
        List<Category> list = categories.findByBlogIdOrderBySortOrderAsc(blog.getId());
        assertThat(list).singleElement().satisfies(c -> {
            assertThat(c.getName()).isEqualTo("미분류");
            assertThat(c.isDefaultCategory()).isTrue();
        });

        // 같은 요청을 다시 보내면 거절 (인증됨 표시가 지워졌다)
        signup(nickname, email, "abcd123!", "abcd123!", token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 인증_없이_가입하면_403() throws Exception {
        signup(nickname, email, "abcd123!", "abcd123!", "no-token")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message").value("이메일 인증을 먼저 완료해 주세요"));
        assertThat(users.existsActiveByEmail(email)).isFalse();
    }

    @Test
    void 이메일을_바꾸면_인증이_취소된다() throws Exception {
        String token = sendAndGetToken(nickname, email);
        confirm(email, mail.lastCode.get(email), token).andExpect(status().isOk());
        cancel(email, token).andExpect(status().isNoContent());

        signup(nickname, email, "abcd123!", "abcd123!", token).andExpect(status().isForbidden());
    }

    @Test
    void 인증한_사람의_증표가_아니면_가입할_수_없다() throws Exception {
        String token = sendAndGetToken(nickname, email);
        confirm(email, mail.lastCode.get(email), token).andExpect(status().isOk());

        // 남이 같은 이메일로 먼저 가입하려 해도(선점) 증표가 없으면 거절
        signup("other" + nickname.substring(1, 5), email, "zzzz999!", "zzzz999!", "stolen")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        // 남이 인증을 취소하려 해도 증표가 없으면 아무 일도 없다
        cancel(email, "stolen").andExpect(status().isNoContent());

        signup(nickname, email, "abcd123!", "abcd123!", token).andExpect(status().isCreated());
    }

    @Test
    void 예전_증표로는_새_인증을_취소할_수_없다() throws Exception {
        String oldToken = sendAndGetToken(nickname, email);
        confirm(email, mail.lastCode.get(email), oldToken).andExpect(status().isOk());
        store.releaseCooldown(email, oldToken); // 1분 기다린 셈 치고 다시 받는다
        String newToken = sendAndGetToken(nickname, email);

        store.releaseCooldown(email, oldToken); // 예전 요청은 새 요청의 1분 막기를 풀 수 없다
        sendCode(nickname, email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"));
        cancel(email, oldToken).andExpect(status().isNoContent()); // 아무것도 지우지 않는다
        confirm(email, mail.lastCode.get(email), newToken).andExpect(status().isOk());
        // 새로 받으면 예전 인증은 무효: 예전 증표로는 가입할 수 없다
        signup(nickname, email, "abcd123!", "abcd123!", oldToken).andExpect(status().isForbidden());
        signup(nickname, email, "abcd123!", "abcd123!", newToken).andExpect(status().isCreated());
    }

    @Test
    void 인증번호와_증표는_원문으로_저장하지_않는다() throws Exception {
        String token = sendAndGetToken(nickname, email);
        String savedCode = redis.opsForValue().get("emailauth:code:" + email);
        String savedToken = redis.opsForValue().get("emailauth:flow:" + email);

        assertThat(savedCode).isNotEqualTo(mail.lastCode.get(email)).hasSize(64);
        assertThat(savedToken).isNotEqualTo(token).hasSize(64);
    }

    @Test
    void 증표가_다르면_인증번호를_확인할_수_없다() throws Exception {
        sendAndGetToken(nickname, email);
        confirm(email, mail.lastCode.get(email), "stolen")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
    }

    @Test
    void 가입_칸을_어기면_칸마다_이유를_알려_준다() throws Exception {
        signup("철", "not-an-email", "short", "different", "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("nickname", "email", "password", "passwordConfirm")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'nickname')].message")
                        .value("닉네임은 한글, 영문, 숫자로 2~10자여야 합니다"));
    }

    @Test
    void 인증번호_받기는_형식과_중복을_먼저_본다() throws Exception {
        sendCode(nickname, "bad").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
        sendCode("철!", email).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_NICKNAME"));

        users.saveAndFlush(User.create(email, "hash", nickname));
        sendCode("other" + nickname.substring(1, 5), email.toUpperCase())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        assertThat(mail.lastCode).doesNotContainKey(email); // 가입된 이메일에는 메일을 보내지 않는다
    }

    @Test
    void 일분_안에_다시_받으면_거절() throws Exception {
        sendCode(nickname, email).andExpect(status().isOk());
        sendCode(nickname, email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"));
    }

    @Test
    void 메일_발송이_실패하면_바로_다시_받을_수_있다() throws Exception {
        mail.fail = true;
        sendCode(nickname, email)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MAIL_SEND_FAILED"));

        mail.fail = false;
        sendCode(nickname, email).andExpect(status().isOk()); // 1분 제한에 걸리지 않는다
    }

    @Test
    void 틀린_번호를_5번_넣으면_번호가_폐기된다() throws Exception {
        String token = sendAndGetToken(nickname, email);
        String right = mail.lastCode.get(email);
        String wrong = right.equals("AAAAAA") ? "BBBBBB" : "AAAAAA";

        for (int i = 1; i <= 4; i++) {
            confirm(email, wrong, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_MISMATCH"));
        }
        confirm(email, wrong, token).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("CODE_ATTEMPTS_EXCEEDED"));
        confirm(email, right, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
    }

    @Test
    void 인증번호는_한_번만_쓰고_소문자로_넣어도_된다() throws Exception {
        String token = sendAndGetToken(nickname, email);
        String code = mail.lastCode.get(email);

        confirm(email, code.toLowerCase(), token).andExpect(status().isOk());
        confirm(email, code, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
    }

    private ResultActions sendCode(String nick, String mailAddress) throws Exception {
        return mvc.perform(post("/api/auth/email-verifications").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("nickname", nick, "email", mailAddress))));
    }

    /** 인증번호를 받고, 응답의 인증 증표를 돌려준다. */
    private String sendAndGetToken(String nick, String mailAddress) throws Exception {
        String body = sendCode(nick, mailAddress).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"verificationToken\":\"([^\"]+)\".*", "$1");
        assertThat(token).isNotBlank().doesNotContain("{");
        return token;
    }

    private ResultActions confirm(String mailAddress, String code, String token) throws Exception {
        return mvc.perform(post("/api/auth/email-verifications/confirm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", mailAddress, "code", code, "verificationToken", token))));
    }

    private ResultActions cancel(String mailAddress, String token) throws Exception {
        return mvc.perform(post("/api/auth/email-verifications/cancel").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email", mailAddress, "verificationToken", token))));
    }

    private ResultActions signup(String nick, String mailAddress, String password, String confirm, String token) throws Exception {
        return mvc.perform(post("/api/auth/signup").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nickname", nick, "email", mailAddress, "password", password,
                        "passwordConfirm", confirm, "verificationToken", token))));
    }

    /** 테스트 값에는 따옴표·역슬래시가 없어서 간단히 만든다. */
    private static String json(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        fields.forEach((k, v) -> {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append('"').append(k).append("\":\"").append(v).append('"');
        });
        return sb.append('}').toString();
    }
}
