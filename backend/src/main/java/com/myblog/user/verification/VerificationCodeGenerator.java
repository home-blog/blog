package com.myblog.user.verification;

import com.myblog.user.config.AuthProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 인증번호 만들기 (FR-014, NF-05): 영문 대문자+숫자, 헷갈리는 O, 0, I, 1은 뺀다. 길이는 설정값(6자리).
 * 예측할 수 없도록 SecureRandom을 쓴다.
 */
@Component
public class VerificationCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public VerificationCodeGenerator(AuthProperties properties) {
        this.length = properties.emailVerification().codeLength();
    }

    public String generate() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
