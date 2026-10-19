package com.myblog.ai.service;

import com.myblog.ai.config.ProofreadProperties;
import com.myblog.ai.service.ProofreadChecker.Correction;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 빨간 펜 교정 (specs/014 US1).
 *
 * <ul>
 *   <li>하루 횟수는 Redis에 한국 날짜 키로 센다(D-5). 먼저 하나 올리고, 넘었거나 AI가 실패하면 되돌린다(S1-4, S1-5)</li>
 *   <li>같은 본문은 결과를 하루 보관해 AI를 다시 부르지 않고 횟수도 늘리지 않는다(D-6)</li>
 *   <li>AI 답은 {@link ProofreadChecker}가 본문에서 다시 찾아 검사한다(S1-2, S1-3)</li>
 * </ul>
 * Redis의 값은 잃어도 된다(헌법 1.1.0): 비면 하루 횟수가 처음부터 다시 셀 뿐이다.
 */
@Service
public class ProofreadService {

    private static final Logger log = LoggerFactory.getLogger(ProofreadService.class);

    private final Proofreader proofreader;
    private final ProofreadChecker checker;
    private final ProofreadProperties properties;
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public ProofreadService(Proofreader proofreader, ProofreadChecker checker, ProofreadProperties properties,
            StringRedisTemplate redis, JsonMapper jsonMapper, Clock clock) {
        this.proofreader = proofreader;
        this.checker = checker;
        this.properties = properties;
        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /** 글쓰기 화면이 단추를 그릴지 정하는 값 (contracts 1). */
    public record Status(boolean enabled, int dailyLimit, int remaining, int maxLength) {
    }

    /** 교정 결과. remaining은 오늘 남은 횟수, cached는 같은 본문의 앞 결과를 다시 쓴 것. */
    public record Result(List<Correction> corrections, int remaining, boolean cached) {
    }

    public Status status(Long memberId) {
        return new Status(proofreader.available(), properties.dailyLimit(), remaining(memberId), properties.maxLength());
    }

    public Result proofread(Long memberId, String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException(ErrorCode.PROOFREAD_EMPTY);
        }
        if (content.length() > properties.maxLength()) {
            throw new ApiException(ErrorCode.PROOFREAD_TOO_LONG,
                    ErrorCode.PROOFREAD_TOO_LONG.message().formatted(properties.maxLength()));
        }
        if (!proofreader.available()) {
            throw new ApiException(ErrorCode.PROOFREAD_UNAVAILABLE);
        }
        String resultKey = "proofread:result:" + sha256(content);
        List<Correction> cached = readCached(resultKey);
        if (cached != null) {
            return new Result(cached, remaining(memberId), true);
        }
        String countKey = countKey(memberId);
        long used;
        try {
            used = take(countKey);
        } catch (RuntimeException e) {
            // 횟수를 셀 수 없으면 AI를 부르지 않는다 (하루 한도를 지킬 수 없다)
            log.warn("빨간 펜 횟수를 세지 못했습니다: {}", e.toString());
            throw new ApiException(ErrorCode.PROOFREAD_UNAVAILABLE);
        }
        if (used > properties.dailyLimit()) {
            redis.opsForValue().decrement(countKey);
            throw new ApiException(ErrorCode.PROOFREAD_LIMIT_EXCEEDED,
                    ErrorCode.PROOFREAD_LIMIT_EXCEEDED.message().formatted(properties.dailyLimit()));
        }
        List<Correction> corrections;
        try {
            corrections = checker.check(content, proofreader.proofread(content));
        } catch (RuntimeException e) {
            redis.opsForValue().decrement(countKey);
            log.warn("빨간 펜을 쓰지 못했습니다: {}", e.getMessage());
            throw new ApiException(ErrorCode.PROOFREAD_UNAVAILABLE);
        }
        writeCached(resultKey, corrections);
        return new Result(corrections, (int) Math.max(0, properties.dailyLimit() - used), false);
    }

    private int remaining(Long memberId) {
        String value = redis.opsForValue().get(countKey(memberId));
        long used = value == null ? 0 : Long.parseLong(value);
        return (int) Math.max(0, properties.dailyLimit() - used);
    }

    /**
     * 오늘 횟수를 하나 올리고 올린 값을 돌려준다. 처음 만든 키에는 다음 한국 자정 + 여유까지의 수명을 준다.
     * 수명을 주지 못하면 올린 것을 되돌린다(수명 없는 키가 남지 않게).
     */
    private long take(String countKey) {
        Long used = redis.opsForValue().increment(countKey);
        if (used != null && used == 1) {
            try {
                Instant now = clock.instant();
                Instant tomorrow = today().plusDays(1).atStartOfDay(properties.zone()).toInstant();
                redis.expire(countKey, Duration.between(now, tomorrow).plus(properties.countMargin()));
            } catch (RuntimeException e) {
                redis.delete(countKey);
                throw e;
            }
        }
        return used == null ? 1 : used;
    }

    private String countKey(Long memberId) {
        return "proofread:count:" + memberId + ":" + today();
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), properties.zone());
    }

    /** 보관한 결과. 없거나 Redis를 읽지 못하면 null(AI를 부른다). */
    private List<Correction> readCached(String key) {
        String json;
        try {
            json = redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("빨간 펜 결과 보관을 읽지 못했습니다: {}", e.toString());
            return null;
        }
        if (json == null) {
            return null;
        }
        try {
            return List.of(jsonMapper.readValue(json, Correction[].class));
        } catch (JacksonException e) {
            redis.delete(key);
            return null;
        }
    }

    /** 결과를 보관한다. 실패해도 이미 받은 교정은 돌려준다(보관은 잃어도 되는 값). */
    private void writeCached(String key, List<Correction> corrections) {
        try {
            redis.opsForValue().set(key, jsonMapper.writeValueAsString(corrections), properties.cacheTtl());
        } catch (RuntimeException e) {
            log.warn("빨간 펜 결과를 보관하지 못했습니다: {}", e.toString());
        }
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
