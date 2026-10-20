package com.myblog.user.oauth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 잃어도 되는 짧은 값 (specs/023 D-3, 헌법 1.1.0): 위임장을 기다리는 인가 요청(10분), 허락 코드(5분, 한 번만),
 * 이미 쓴 코드 표시(코드를 두 번 쓰면 그 연결을 끊는다), 동적 등록 횟수.
 * Redis가 응답하지 않으면 {@link OAuthError#unavailable()}을 던진다(503 temporarily_unavailable).
 */
@Component
class OAuthPending {

    private static final String PREFIX = "myblog:oauth:";
    private static final RedisScript<Long> COUNT = RedisScript.of("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 or redis.call('TTL', KEYS[1]) == -1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return n""", Long.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final OAuthProperties properties;

    OAuthPending(StringRedisTemplate redis, JsonMapper json, OAuthProperties properties) {
        this.redis = redis;
        this.json = json;
        this.properties = properties;
    }

    /** 위임장을 기다리는 인가 요청. redirectUriProvided: 요청에 redirect_uri가 있었나(토큰 요청에서도 같아야 한다). */
    record PendingRequest(String clientId, String clientName, String redirectUri, boolean redirectUriProvided,
            String state, String codeChallenge, String scope, String resource, Instant expiresAt) {
    }

    /** 허락 코드가 가리키는 것. */
    record IssuedCode(long memberId, String clientId, String redirectUri, boolean redirectUriProvided,
            String codeChallenge, String scope, String resource) {
    }

    String saveRequest(PendingRequest request) {
        String id = OAuthTokens.random("", 24);
        set(PREFIX + "req:" + id, request, properties.requestTtl());
        return id;
    }

    Optional<PendingRequest> request(String id) {
        if (!isToken(id)) {
            return Optional.empty();
        }
        return read(guard(() -> redis.opsForValue().get(PREFIX + "req:" + id)), PendingRequest.class);
    }

    /** 허락·거절은 요청을 한 번만 꺼낸다(두 번 누르거나 두 탭에서 눌러도 코드는 하나). */
    Optional<PendingRequest> takeRequest(String id) {
        if (!isToken(id)) {
            return Optional.empty();
        }
        return read(guard(() -> redis.opsForValue().getAndDelete(PREFIX + "req:" + id)), PendingRequest.class);
    }

    String saveCode(IssuedCode code) {
        String value = OAuthTokens.random("", 32);
        set(PREFIX + "code:" + OAuthTokens.sha256Hex(value), code, properties.codeTtl());
        return value;
    }

    Optional<IssuedCode> takeCode(String code) {
        if (code == null || code.isBlank() || code.length() > 200) {
            return Optional.empty();
        }
        return read(guard(() -> redis.opsForValue().getAndDelete(PREFIX + "code:" + OAuthTokens.sha256Hex(code))),
                IssuedCode.class);
    }

    /** 코드로 연결을 만들었다고 남긴다: 같은 코드가 다시 오면 그 연결을 끊는다 (S1-4). 코드 수명의 두 배만 둔다. */
    void markCodeUsed(String code, long grantId) {
        guard(() -> {
            redis.opsForValue().set(PREFIX + "used:" + OAuthTokens.sha256Hex(code), String.valueOf(grantId),
                    properties.codeTtl().multipliedBy(2));
            return null;
        });
    }

    Optional<Long> usedCodeGrant(String code) {
        if (code == null || code.isBlank() || code.length() > 200) {
            return Optional.empty();
        }
        String value = guard(() -> redis.opsForValue().get(PREFIX + "used:" + OAuthTokens.sha256Hex(code)));
        return Optional.ofNullable(value).map(Long::valueOf);
    }

    /** 이 IP의 이번 시간 등록 횟수(이번 것 포함). */
    long countRegistration(String ip) {
        Long count = guard(() -> redis.execute(COUNT, List.of(PREFIX + "register:" + ip),
                String.valueOf(Duration.ofHours(1).toSeconds())));
        if (count == null) {
            throw OAuthError.unavailable();
        }
        return count;
    }

    private void set(String key, Object value, Duration ttl) {
        String text = json.writeValueAsString(value);
        guard(() -> {
            redis.opsForValue().set(key, text, ttl);
            return null;
        });
    }

    private <T> Optional<T> read(String text, Class<T> type) {
        return text == null ? Optional.empty() : Optional.of(json.readValue(text, type));
    }

    /** 요청 번호는 우리가 만든 base64url 32글자뿐이다. 다른 모양은 Redis에 묻지도 않는다. */
    private static boolean isToken(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{32}");
    }

    private static <T> T guard(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            throw OAuthError.unavailable();
        }
    }
}
