package com.myblog.user.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.user.config.AuthProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 입력 규칙 (specs/001 FR-003, FR-005 ~ FR-007). 값은 application.yml의 기본값과 같다. */
class UserInputRulesTest {

    private final UserInputRules rules = new UserInputRules(new AuthProperties(
            new AuthProperties.Nickname(2, 10),
            new AuthProperties.Password(8, 20, "!@#$%^&*()_+-="),
            new AuthProperties.EmailVerification(6, Duration.ofMinutes(10), Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(30)),
            new AuthProperties.Login(5, Duration.ofMinutes(10)),
            new AuthProperties.Session(Duration.ofDays(7), Duration.ofDays(30))));

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "User.Name+tag@example.com", "  chulsoo@naver.com  "})
    void 이메일_형식이면_통과(String email) {
        assertThat(rules.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain", "a@b", "a@b.c", "@example.com", "a b@example.com"})
    void 이메일_형식이_아니면_거절(String email) {
        assertThat(rules.isValidEmail(email)).isFalse();
    }

    @Test
    void 이메일이_없으면_거절() {
        assertThat(rules.isValidEmail(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"철수", "chulsoo", "철수99", "ab", "가나다라마바사아자차"})
    void 닉네임_규칙에_맞으면_통과(String nickname) {
        assertThat(rules.isValidNickname(nickname)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"철", "가나다라마바사아자차카", "철 수", "철수!", "ㅊㅅ", ""})
    void 닉네임_규칙에_어긋나면_거절(String nickname) {
        assertThat(rules.isValidNickname(nickname)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd123!", "Passw0rd-=", "a1!a1!a1!a1!a1!a1!a1", "x9^()_+&*"})
    void 비밀번호_규칙에_맞으면_통과(String password) {
        assertThat(rules.isValidPassword(password)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc12!",                 // 8자 미만
        "a1!a1!a1!a1!a1!a1!a1!",  // 20자 초과
        "abcdefgh!",              // 숫자 없음
        "12345678!",              // 영문 없음
        "abcd1234",               // 특수문자 없음
        "abcd 123!",              // 공백
        "abcd123?",               // 허용하지 않는 특수문자
        "비밀번호abc1!"            // 한글
    })
    void 비밀번호_규칙에_어긋나면_거절(String password) {
        assertThat(rules.isValidPassword(password)).isFalse();
    }

    @Test
    void 비밀번호_확인은_같아야_통과() {
        assertThat(rules.isPasswordConfirmed("abcd123!", "abcd123!")).isTrue();
        assertThat(rules.isPasswordConfirmed("abcd123!", "abcd123@")).isFalse();
        assertThat(rules.isPasswordConfirmed(null, null)).isFalse();
    }
}
