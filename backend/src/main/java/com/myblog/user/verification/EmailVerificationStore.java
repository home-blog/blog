package com.myblog.user.verification;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * 인증번호와 증표는 원문 대신 <b>서버 비밀키로 만든 HMAC-SHA-256</b>으로 저장한다. 인증번호는 6자리라 경우의 수가 적어서,
 * 비밀키 없는 해시는 Redis 값을 읽은 사람이 하나씩 대입해 맞힐 수 있다 (2026-10-08 리뷰 반영).
 * 비밀키는 환경 변수 VERIFICATION_SECRET(myblog.verification.secret)로 넣는다. 개발에서 비어 있으면 켤 때마다 새로 만들고,
 * 배포(prod, myblog.verification.require-secret=true)에서 비어 있거나 32자보다 짧으면 서버를 켜지 않는다.
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

    /** 새 흐름으로 한 번에 바꾼다: 이전 틀린 횟수·인증됨·증표를 지우고 새 번호와 증표를 같은 만료로 저장한다. */
    private static final RedisScript<Long> START_FLOW = RedisScript.of(
            "redis.call('DEL', KEYS[3], KEYS[4]) "
                    + "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3]) "
                    + "redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3]) "
                    + "return 1",
            Long.class);

    /**
     * 인증번호 확인을 한 번에 처리한다: 증표 확인 → 번호 확인 → (맞으면) 번호·틀린 횟수·증표를 지우고 인증됨 표시 /
     * (틀리면) 틀린 횟수 +1, 한도에 닿으면 번호 폐기. 다른 스크립트(새 흐름 시작, 이메일 변경)와 섞이지 않는다.
     * 결과: 0 번호 없음(만료·폐기·증표 불일치), 1 맞음, 2 틀림, 3 틀린 횟수 초과
     */
    private static final RedisScript<Long> CONFIRM = RedisScript.of(
            "if redis.call('GET', KEYS[2]) ~= ARGV[2] then return 0 end "
                    + "local code = redis.call('GET', KEYS[1]) "
                    + "if not code then return 0 end "
                    + "if code == ARGV[1] then "
                    + "  redis.call('DEL', KEYS[1], KEYS[2], KEYS[3]) "
                    + "  redis.call('SET', KEYS[4], ARGV[2], 'PX', ARGV[5]) "
                    + "  return 1 "
                    + "end "
                    + "local c = redis.call('INCR', KEYS[3]) "
                    + "if c == 1 then redis.call('PEXPIRE', KEYS[3], ARGV[4]) end "
                    + "if c >= tonumber(ARGV[3]) then redis.call('DEL', KEYS[1], KEYS[3]) return 3 end "
                    + "return 2",
            Long.class);

    /** 1분 막기를 건 그 요청(증표가 같은 요청)일 때만 푼다. 그사이 막기가 끝나 다른 요청이 새로 건 막기는 건드리지 않는다. */
    private static final RedisScript<Long> RELEASE_COOLDOWN_IF_OWNER = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0",
            Long.class);

    /** 인증번호 확인 결과 */
    public enum ConfirmResult { NO_CODE, MATCH, MISMATCH, ATTEMPTS_EXCEEDED }

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationStore.class);
    private static final String HMAC = "HmacSHA256";

    private final StringRedisTemplate redis;
    private final SecretKeySpec secretKey;

    /** 배포에서는 비밀키가 꼭 있어야 한다. 서버마다 다른 임시 키를 쓰면 한 서버가 저장한 인증을 다른 서버가 확인하지 못한다. */
    static final int MIN_SECRET_LENGTH = 32;

    public EmailVerificationStore(StringRedisTemplate redis, @Value("${myblog.verification.secret:}") String secret,
            @Value("${myblog.verification.require-secret:false}") boolean requireSecret) {
        this.redis = redis;
        boolean blank = secret == null || secret.isBlank();
        if (requireSecret && (blank || secret.length() < MIN_SECRET_LENGTH)) {
            // 배포 설정(prod)에서는 켜지 않고 바로 멈춘다: 잘못된 설정을 늦게 알게 되는 것보다 낫다
            throw new IllegalStateException("myblog.verification.secret(VERIFICATION_SECRET)에 " + MIN_SECRET_LENGTH
                    + "자 이상의 무작위 값을 넣어야 서버를 켤 수 있습니다");
        }
        byte[] keyBytes;
        if (blank) {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("myblog.verification.secret이 비어 있어 임시 비밀키를 만들었습니다. 서버를 다시 켜면 진행 중인 이메일 인증은 처음부터 해야 합니다");
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = new SecretKeySpec(keyBytes, HMAC);
    }

    /**
     * 새 인증을 시작한다: 번호와 증표를 해시로 저장한다. 이전 번호·증표는 덮어써서 무효가 되고,
     * 틀린 횟수와 이전 인증의 "인증됨" 표시도 지운다 (FR-017. 이전 증표로 새 흐름을 건드릴 수 없게).
     * 모두 Lua 한 번으로 실행해, 중간에 끊겨도 새 번호와 이전 증표가 섞여 남지 않는다.
     */
    public void startFlow(String email, String code, String token, Duration ttl) {
        redis.execute(START_FLOW,
                List.of(key("code", email), key("flow", email), key("fail", email), key("verified", email)),
                hash(code), hash(token), String.valueOf(ttl.toMillis()));
    }

    /**
     * 인증번호를 확인한다 (한 번에, CONFIRM 스크립트). 증표가 그 흐름의 것이 아니면 번호가 없는 것과 같게 본다.
     * 맞으면 인증됨 표시(증표의 해시)를 verifiedTtl 동안 남긴다.
     */
    public ConfirmResult confirm(String email, String code, String token, int maxWrongAttempts, Duration codeTtl,
            Duration verifiedTtl) {
        if (token == null) {
            return ConfirmResult.NO_CODE;
        }
        Long result = redis.execute(CONFIRM,
                List.of(key("code", email), key("flow", email), key("fail", email), key("verified", email)),
                hash(code), hash(token), String.valueOf(maxWrongAttempts), String.valueOf(codeTtl.toMillis()),
                String.valueOf(verifiedTtl.toMillis()));
        if (result == null) {
            return ConfirmResult.NO_CODE;
        }
        return switch (result.intValue()) {
            case 1 -> ConfirmResult.MATCH;
            case 2 -> ConfirmResult.MISMATCH;
            case 3 -> ConfirmResult.ATTEMPTS_EXCEEDED;
            default -> ConfirmResult.NO_CODE;
        };
    }

    /**
     * 1분 막기를 건다. 이미 걸려 있으면 false. 확인과 걸기를 한 번에 해서(SET NX)
     * 동시에 여러 요청이 와도 하나만 통과한다 (FR-018). 값에는 이 요청의 증표 해시를 담아 "누가 걸었는지"를 남긴다.
     */
    public boolean tryStartCooldown(String email, String token, Duration cooldown) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key("cooldown", email), hash(token), cooldown));
    }

    /** 메일을 보내지 못했거나 하루 한도에 걸렸을 때, 내가 건 1분 막기만 푼다 (FR-020: 바로 다시 받을 수 있게). */
    public void releaseCooldown(String email, String token) {
        redis.execute(RELEASE_COOLDOWN_IF_OWNER, List.of(key("cooldown", email)), hash(token));
    }

    public long sentCount(String email) {
        String value = redis.opsForValue().get(key("daily", email));
        return value == null ? 0L : Long.parseLong(value);
    }

    /** 메일 발송에 성공한 뒤에만 부른다: 보낸 횟수를 올린다 (FR-018, FR-020). */
    public void recordSent(String email) {
        increment(key("daily", email), DAILY_WINDOW);
    }

    /** 인증을 마쳤고, 그때 받은 증표와 같은지. */
    public boolean isVerified(String email, String token) {
        return token != null && hash(token).equals(redis.opsForValue().get(key("verified", email)));
    }

    public void clearVerified(String email) {
        redis.delete(key("verified", email));
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

    /** 서버 비밀키로 HMAC-SHA-256을 낸다 (16진수 64자). 비밀키가 없으면 Redis 값만으로는 원래 번호를 맞혀 볼 수 없다. */
    String hash(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256을 쓸 수 없습니다", e);
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
