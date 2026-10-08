package com.myblog.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 회원 가입·로그인 규칙의 숫자를 한곳에서 읽는다 (application.yml의 auth.*, specs/001 plan.md 설정값 목록).
 * 코드에 숫자를 박지 않고 이 값을 쓴다 (NF-08).
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Nickname nickname,
        Password password,
        EmailVerification emailVerification,
        Login login,
        Session session) {

    /** 닉네임 글자 수 (2~10자). */
    public record Nickname(int minLength, int maxLength) {
    }

    /** 비밀번호 글자 수 (8~20자)와 쓸 수 있는 특수문자. */
    public record Password(int minLength, int maxLength, String allowedSpecials) {
    }

    /** 이메일 인증: 번호 길이, 유효 시간, 다시 받기 간격, 하루 횟수, 틀린 횟수, 인증됨 유지 시간. */
    public record EmailVerification(
            int codeLength,
            Duration codeTtl,
            Duration resendInterval,
            int dailyLimit,
            int maxWrongAttempts,
            Duration verifiedTtl) {
    }

    /** 로그인 연속 실패 횟수와 잠금 시간. */
    public record Login(int maxFailedAttempts, Duration lockDuration) {
    }

    /** 로그인 유지: 마지막 사용 후 7일, 로그인한 때부터 최대 30일. */
    public record Session(Duration idleTimeout, Duration absoluteTimeout) {
    }
}
