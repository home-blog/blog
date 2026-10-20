package com.myblog.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myblog.connect.config.McpProperties;
import com.myblog.connect.config.McpProperties.RateLimit;
import com.myblog.connect.config.McpProperties.Search;
import com.myblog.connect.service.McpRateLimiter.Verdict;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 요청 수를 셀 수 없을 때 (specs/020 D-5): Redis가 멈추거나 값을 돌려주지 않으면 막는다(503).
 * 열린 채 두면 열쇠를 가진 쪽이 그동안 수 제한 없이 검색을 퍼부을 수 있다.
 */
class McpRateLimiterTest {

    private static final McpProperties PROPERTIES = new McpProperties(new RateLimit(2), new Search(50, 600, 8, 2, 10));

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final McpRateLimiter limiter = new McpRateLimiter(redis, PROPERTIES);

    @SuppressWarnings("unchecked")
    private void redisAnswers(Long count) {
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(count);
    }

    @Test
    void 한도_안이면_받고_넘으면_LIMITED() {
        redisAnswers(2L);
        assertThat(limiter.check(1L)).isEqualTo(Verdict.ALLOWED);
        redisAnswers(3L);
        assertThat(limiter.check(1L)).isEqualTo(Verdict.LIMITED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void Redis가_멈추거나_값을_돌려주지_않으면_막는다() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));
        assertThat(limiter.check(1L)).isEqualTo(Verdict.UNAVAILABLE);
        redisAnswers(null);
        assertThat(limiter.check(1L)).isEqualTo(Verdict.UNAVAILABLE);
    }
}
