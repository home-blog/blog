package com.myblog.user.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.user.config.AuthProperties;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 인증번호 형식 (FR-014): 영문 대문자+숫자 6자리, O·0·I·1 없음. */
class VerificationCodeGeneratorTest {

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator(new AuthProperties(
            new AuthProperties.Nickname(2, 10),
            new AuthProperties.Password(8, 20, "!@#$%^&*()_+-="),
            new AuthProperties.EmailVerification(6, Duration.ofMinutes(10), Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(30)),
            new AuthProperties.Login(5, Duration.ofMinutes(10)),
            new AuthProperties.Session(Duration.ofDays(7), Duration.ofDays(30))));

    @Test
    void 영문_대문자와_숫자_6자리이고_헷갈리는_글자가_없다() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            assertThat(code).matches("^[A-Z2-9]{6}$").doesNotContain("O", "0", "I", "1");
            seen.add(code);
        }
        assertThat(seen).hasSizeGreaterThan(990); // 거의 겹치지 않는다
    }
}
