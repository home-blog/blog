package com.myblog.user.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** 무작위 값, SHA-256, PKCE(S256) 확인 (specs/023 D-2). */
final class OAuthTokens {

    static final String ACCESS_PREFIX = "go_";
    static final String REFRESH_PREFIX = "gr_";
    static final String CLIENT_PREFIX = "gc_";
    /** RFC 7636 4.1: code_verifier는 43~128자의 unreserved 글자. code_challenge(S256)도 43자 base64url. */
    private static final Pattern VERIFIER = Pattern.compile("[A-Za-z0-9._~-]{43,128}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private OAuthTokens() {
    }

    static String random(String prefix, int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    static String sha256Hex(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    static boolean isVerifier(String value) {
        return value != null && VERIFIER.matcher(value).matches();
    }

    /** code_challenge = BASE64URL(SHA256(code_verifier)). 비교는 시간이 새지 않게 한다. */
    static boolean verifierMatches(String verifier, String challenge) {
        if (!isVerifier(verifier) || challenge == null) {
            return false;
        }
        byte[] expected = Base64.getUrlEncoder().withoutPadding().encodeToString(digest(verifier))
                .getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, challenge.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }
}
