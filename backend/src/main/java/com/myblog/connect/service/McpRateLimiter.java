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
 * 잃어도 되는 짧은 값이라 Redis에 둔다(헌법 1.1.0). Redis가 안 되면 막지 않고 통과한다(서비스가 멈추지 않게).
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
    public boolean allow(Long keyId) {
        String key = "myblog:mcp:rate:" + keyId;
        try {
            Long count = redis.execute(COUNT, List.of(key), String.valueOf(WINDOW.toSeconds()));
            return count == null || count <= perMinute;
        } catch (RuntimeException e) {
            log.warn("MCP 요청 수를 세지 못해 통과시킵니다: {}", e.toString());
            return true;
        }
    }
}
