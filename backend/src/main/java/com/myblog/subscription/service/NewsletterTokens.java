package com.myblog.subscription.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 원고 편지의 그만 받기 값 (specs/016 D-7, S3-3): "받아 보기 번호.서명". 서명은 서버 비밀키(인증번호와 같은 VERIFICATION_SECRET)로 만든
 * HMAC-SHA-256이고, 용도 이름을 앞에 붙여 다른 서명과 섞이지 않는다. 비밀키가 비어 있으면(개발) 켤 때마다 새로 만든다.
 */
@Component
public class NewsletterTokens {

    private static final String HMAC = "HmacSHA256";
    private static final String PURPOSE = "newsletter-unsubscribe:";

    private final SecretKeySpec key;

    public NewsletterTokens(@Value("${myblog.verification.secret:}") String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = new SecretKeySpec(bytes, HMAC);
    }

    public String sign(long subscriptionId) {
        return subscriptionId + "." + signature(subscriptionId);
    }

    /** 서명이 맞으면 받아 보기 번호. 모양이 틀리거나 서명이 다르면 빈 값. */
    public Optional<Long> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return Optional.empty();
        }
        long id;
        try {
            id = Long.parseLong(token.substring(0, dot));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        byte[] expected = signature(id).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = token.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual) ? Optional.of(id) : Optional.empty();
    }

    private String signature(long subscriptionId) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(key);
            byte[] digest = mac.doFinal((PURPOSE + subscriptionId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
