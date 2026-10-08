package com.myblog.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 계정 관리 규칙의 숫자를 한곳에서 읽는다 (application.yml의 account.*, specs/002 plan.md 설정값 목록).
 * 닉네임·비밀번호 규칙은 가입과 같아서 AuthProperties를 그대로 쓴다.
 */
@ConfigurationProperties(prefix = "account")
public record AccountProperties(Intro intro) {

    /** 소개 글자 수 (0~100자). 글자는 코드 포인트로 센다 (이모지 하나 = 한 글자). */
    public record Intro(int minLength, int maxLength) {
    }
}
