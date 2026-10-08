package com.myblog.user.verification;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 흐름 값을 Redis에 둔다 (specs/001 data-model 3, 헌법 1.1.0: 잃어도 다시 하면 되는 짧은 값).
 * 이메일은 항상 소문자로 맞춘 값을 받는다.
 *
 * <pre>
 * emailauth:code:{email}      인증번호        10분
 * emailauth:fail:{email}      틀린 횟수       인증번호와 같은 10분
 * emailauth:cooldown:{email}  다시 받기 막기  1분 (발송 성공 뒤)
 * emailauth:daily:{email}     보낸 횟수       첫 발송부터 24시간
 * emailauth:verified:{email}  인증됨 표시     30분
 * </pre>
 */
@Component
public class EmailVerificationStore {

    /** "하루 5번"은 달력 날짜가 아니라 첫 발송부터 24시간으로 센다 (data-model 3). */
    static final Duration DAILY_WINDOW = Duration.ofHours(24);

    private static final String PREFIX = "emailauth:";

    private final StringRedisTemplate redis;

    public EmailVerificationStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 새 번호를 저장한다. 이전 번호는 덮어써서 무효가 되고, 틀린 횟수는 0으로 돌아간다 (FR-017). */
    public void saveCode(String email, String code, Duration ttl) {
        redis.opsForValue().set(key("code", email), code, ttl);
        redis.delete(key("fail", email));
    }

    public Optional<String> findCode(String email) {
        return Optional.ofNullable(redis.opsForValue().get(key("code", email)));
    }

    /** 번호와 틀린 횟수를 지운다. */
    public void deleteCode(String email) {
        redis.delete(key("code", email));
        redis.delete(key("fail", email));
    }

    /** 틀린 횟수를 하나 올리고 올린 뒤의 값을 돌려준다. 처음 틀릴 때 만료를 건다. */
    public long incrementFailures(String email, Duration ttl) {
        String key = key("fail", email);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttl);
        }
        return count == null ? 0L : count;
    }

    public boolean isCoolingDown(String email) {
        return Boolean.TRUE.equals(redis.hasKey(key("cooldown", email)));
    }

    public long sentCount(String email) {
        String value = redis.opsForValue().get(key("daily", email));
        return value == null ? 0L : Long.parseLong(value);
    }

    /** 메일 발송에 성공한 뒤에만 부른다: 1분 막기를 걸고 보낸 횟수를 올린다 (FR-018, FR-020). */
    public void recordSent(String email, Duration cooldown) {
        redis.opsForValue().set(key("cooldown", email), "1", cooldown);
        String daily = key("daily", email);
        Long count = redis.opsForValue().increment(daily);
        if (count != null && count == 1L) {
            redis.expire(daily, DAILY_WINDOW);
        }
    }

    public void markVerified(String email, Duration ttl) {
        redis.opsForValue().set(key("verified", email), "1", ttl);
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redis.hasKey(key("verified", email)));
    }

    public void clearVerified(String email) {
        redis.delete(key("verified", email));
    }

    /** 이메일 변경: 번호, 틀린 횟수, 인증됨 표시를 지운다. 1분·하루 횟수는 남긴다 (FR-021). */
    public void clearFlow(String email) {
        deleteCode(email);
        clearVerified(email);
    }

    private static String key(String kind, String email) {
        return PREFIX + kind + ":" + email;
    }
}
