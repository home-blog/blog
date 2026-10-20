package com.myblog.connect.service;

import com.myblog.connect.config.McpProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 열쇠마다 요청 수 (specs/020 D-5, S2-3). 첫 요청에서 1분짜리 창을 열고 그 안의 요청을 센다(Redis INCR + EXPIRE).
 * 잃어도 되는 짧은 값이라 Redis에 둔다(헌법 1.1.0). Redis가 안 되면 막지 않고 통과한다(서비스가 멈추지 않게).
 */
@Component
public class McpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(McpRateLimiter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

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
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, WINDOW);
            }
            return count == null || count <= perMinute;
        } catch (RuntimeException e) {
            log.warn("MCP 요청 수를 세지 못해 통과시킵니다: {}", e.toString());
            return true;
        }
    }
}
