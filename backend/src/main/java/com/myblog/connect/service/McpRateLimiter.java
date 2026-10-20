package com.myblog.connect.service;

import com.myblog.connect.config.McpProperties;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 열쇠마다 요청 수 (specs/020 D-5, S2-3). 첫 요청에서 1분짜리 창을 열고 그 안의 요청을 센다(Redis INCR + EXPIRE).
 * 두 명령을 한 스크립트로 묶어, 수만 올라가고 만료가 빠진 키(영원히 429)가 남지 않게 한다. 만료가 없는 키를 만나도 다시 건다.
 * 잃어도 되는 짧은 값이라 Redis에 둔다(헌법 1.1.0). Redis가 안 되면 셀 수 없으니 막는다(503): 열린 채 두면 열쇠를 가진 쪽이
 * 그동안 수 제한 없이 검색을 퍼부을 수 있다. MCP는 곁 기능이라 잠깐 멈춰도 사이트는 그대로다.
 */
@Component
public class McpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(McpRateLimiter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final RedisScript<Long> COUNT = RedisScript.of("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 or redis.call('TTL', KEYS[1]) == -1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return n""", Long.class);

    private final StringRedisTemplate redis;
    private final int perMinute;

    public McpRateLimiter(StringRedisTemplate redis, McpProperties properties) {
        this.redis = redis;
        this.perMinute = properties.rateLimit().perMinute();
    }

    /** 이번 요청을 받아도 되나. */
    public Verdict check(Long keyId) {
        String key = "myblog:mcp:rate:" + keyId;
        try {
            Long count = redis.execute(COUNT, List.of(key), String.valueOf(WINDOW.toSeconds()));
            if (count == null) {
                log.warn("MCP 요청 수를 세지 못해 막습니다: Redis가 값을 돌려주지 않았습니다");
                return Verdict.UNAVAILABLE;
            }
            return count <= perMinute ? Verdict.ALLOWED : Verdict.LIMITED;
        } catch (RuntimeException e) {
            log.warn("MCP 요청 수를 세지 못해 막습니다: {}", e.toString());
            return Verdict.UNAVAILABLE;
        }
    }

    /** ALLOWED 받는다, LIMITED 1분 안에 너무 많다(429), UNAVAILABLE 셀 수 없다(503). */
    public enum Verdict {
        ALLOWED, LIMITED, UNAVAILABLE
    }
}
