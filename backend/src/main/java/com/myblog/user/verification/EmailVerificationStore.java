package com.myblog.user.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 흐름 값을 Redis에 둔다 (specs/001 data-model 3, 헌법 1.1.0: 잃어도 다시 하면 되는 짧은 값).
 * 이메일은 항상 소문자로 맞춘 값을 받는다.
 *
 * <pre>
 * emailauth:code:{email}      인증번호(해시)   10분
 * emailauth:fail:{email}      틀린 횟수       인증번호와 같은 10분
 * emailauth:cooldown:{email}  다시 받기 막기  1분 (발송 성공 뒤)
 * emailauth:daily:{email}     보낸 횟수       첫 발송부터 24시간
 * emailauth:flow:{email}      인증 증표(해시) 인증번호와 같은 10분 (인증번호를 받은 사람만 가진다)
 * emailauth:verified:{email}  인증됨 표시     30분 (값은 인증 증표의 해시. 가입할 때 같은 증표를 내야 한다)
 * </pre>
 *
 * 인증 증표는 "인증번호를 받은 그 사람"이 확인·이메일 변경·가입을 하는지 확인하는 데 쓴다.
 * 남이 같은 이메일로 먼저 가입하거나(선점) 인증을 취소해 방해하는 것을 막는다.
 * 인증번호와 증표는 원문 대신 SHA-256 해시로 저장한다 (Redis 값을 읽어도 그대로 쓸 수 없게).
 */
@Component
public class EmailVerificationStore {

    /** "하루 5번"은 달력 날짜가 아니라 첫 발송부터 24시간으로 센다 (data-model 3). */
    static final Duration DAILY_WINDOW = Duration.ofHours(24);

    private static final String PREFIX = "emailauth:";

    /** 횟수를 올리고, 처음 올린 때만 만료를 건다. 두 명령을 한 번에 실행해 만료 없는 키가 남지 않게 한다. */
    private static final RedisScript<Long> INCREMENT_WITH_TTL = RedisScript.of(
            "local c = redis.call('INCR', KEYS[1]) if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end return c",
            Long.class);

    /**
     * 이메일 변경: 증표가 맞을 때만 흐름의 값을 모두 지운다. 진행 중인 인증번호가 있으면 그 흐름의 증표만 인정한다.
     * 확인과 삭제를 한 번에 해서 그사이에 새 흐름이 끼어들 수 없다.
     */
    private static final RedisScript<Long> CANCEL_IF_OWNER = RedisScript.of(
            "local flow = redis.call('GET', KEYS[1]) "
                    + "local owner "
                    + "if flow then owner = (flow == ARGV[1]) else owner = (redis.call('GET', KEYS[2]) == ARGV[1]) end "
                    + "if owner then redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4]) return 1 end "
                    + "return 0",
            Long.class);

    /** 인증번호 확인 결과 */
    public enum CodeCheck { NONE, MATCH, MISMATCH }

    private final StringRedisTemplate redis;

    public EmailVerificationStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 새 인증을 시작한다: 번호와 증표를 해시로 저장한다. 이전 번호·증표는 덮어써서 무효가 되고,
     * 틀린 횟수와 이전 인증의 "인증됨" 표시도 지운다 (FR-017. 이전 증표로 새 흐름을 건드릴 수 없게).
     */
    public void startFlow(String email, String code, String token, Duration ttl) {
        redis.delete(List.of(key("fail", email), key("verified", email)));
        redis.opsForValue().set(key("code", email), hash(code), ttl);
        redis.opsForValue().set(key("flow", email), hash(token), ttl);
    }

    /** 넣은 번호가 저장한 번호와 같은지. 번호가 없으면(만료·폐기) NONE. */
    public CodeCheck checkCode(String email, String input) {
        String saved = redis.opsForValue().get(key("code", email));
        if (saved == null) {
            return CodeCheck.NONE;
        }
        boolean same = MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8),
                hash(input).getBytes(StandardCharsets.UTF_8));
        return same ? CodeCheck.MATCH : CodeCheck.MISMATCH;
    }

    /** 번호와 틀린 횟수를 지운다. */
    public void deleteCode(String email) {
        redis.delete(List.of(key("code", email), key("fail", email)));
    }

    /** 틀린 횟수를 하나 올리고 올린 뒤의 값을 돌려준다. 처음 틀릴 때 만료를 건다. */
    public long incrementFailures(String email, Duration ttl) {
        return increment(key("fail", email), ttl);
    }

    /**
     * 1분 막기를 건다. 이미 걸려 있으면 false. 확인과 걸기를 한 번에 해서(SET NX)
     * 동시에 여러 요청이 와도 하나만 통과한다 (FR-018).
     */
    public boolean tryStartCooldown(String email, Duration cooldown) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key("cooldown", email), "1", cooldown));
    }

    /** 메일을 보내지 못했거나 하루 한도에 걸렸을 때 1분 막기를 푼다 (FR-020: 바로 다시 받을 수 있게). */
    public void releaseCooldown(String email) {
        redis.delete(key("cooldown", email));
    }

    public long sentCount(String email) {
        String value = redis.opsForValue().get(key("daily", email));
        return value == null ? 0L : Long.parseLong(value);
    }

    /** 메일 발송에 성공한 뒤에만 부른다: 보낸 횟수를 올린다 (FR-018, FR-020). */
    public void recordSent(String email) {
        increment(key("daily", email), DAILY_WINDOW);
    }

    public boolean isFlowToken(String email, String token) {
        return token != null && hash(token).equals(redis.opsForValue().get(key("flow", email)));
    }

    /** 인증 완료: 인증됨 표시에 증표(해시)를 담아 둔다. 인증 단계의 증표는 지운다. */
    public void markVerified(String email, String token, Duration ttl) {
        redis.opsForValue().set(key("verified", email), hash(token), ttl);
        redis.delete(key("flow", email));
    }

    /** 인증을 마쳤고, 그때 받은 증표와 같은지. */
    public boolean isVerified(String email, String token) {
        return token != null && hash(token).equals(redis.opsForValue().get(key("verified", email)));
    }

    public void clearVerified(String email) {
        redis.delete(key("verified", email));
    }

    /** 번호, 틀린 횟수, 증표, 인증됨 표시를 지운다. 1분·하루 횟수는 남긴다 (FR-021). */
    public void clearFlow(String email) {
        redis.delete(List.of(key("code", email), key("fail", email), key("flow", email), key("verified", email)));
    }

    /** 이메일 변경: 그 인증을 시작한 사람(증표가 맞는 사람)일 때만 지운다. 지웠으면 true. */
    public boolean cancelIfOwner(String email, String token) {
        if (token == null) {
            return false;
        }
        Long result = redis.execute(CANCEL_IF_OWNER,
                List.of(key("flow", email), key("verified", email), key("code", email), key("fail", email)), hash(token));
        return result != null && result == 1L;
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }

    private long increment(String key, Duration ttl) {
        Long count = redis.execute(INCREMENT_WITH_TTL, List.of(key), String.valueOf(ttl.toMillis()));
        return count == null ? 0L : count;
    }

    private static String key(String kind, String email) {
        return PREFIX + kind + ":" + email;
    }
}
