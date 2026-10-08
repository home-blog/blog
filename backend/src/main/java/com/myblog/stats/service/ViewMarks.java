package com.myblog.stats.service;

import com.myblog.stats.config.StatsProperties;
import com.myblog.stats.time.ServiceDay;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * "30분 안에 봤다", "오늘 왔다" 표시 (specs/006 T053, D-1, 헌법 1.1.0의 조회수 중복 방지·오늘 방문 표시, data-model 8, research B-3, R-1).
 * Redis에 "없을 때만 저장" + 만료 시간으로 둔다. 처음 만든 요청만 참이다: 같은 사람이 동시에 열어도 한 번만 센다.
 * 잃어도 되는 값이다 (Redis가 비면 한 번 더 셀 뿐이다).
 */
@Component
public class ViewMarks {

    private final StringRedisTemplate redis;
    private final ServiceDay serviceDay;
    private final Duration viewWindow;
    private final Duration visitMargin;

    public ViewMarks(StringRedisTemplate redis, ServiceDay serviceDay, StatsProperties properties) {
        this.redis = redis;
        this.serviceDay = serviceDay;
        this.viewWindow = properties.view().dedupeWindow();
        this.visitMargin = properties.visitor().visitMargin();
    }

    /** 이 사람이 이 글을 dedupe-window 안에 처음 열었으면 참. */
    public boolean firstView(Long postId, String visitorKey) {
        return mark("stats:view:" + postId + ":" + visitorKey, viewWindow);
    }

    /**
     * 이 사람이 이 블로그에 이 한국 날짜에 처음 왔으면 참.
     * 표시는 다음 한국 자정까지 + visit-margin. 서버·Redis 시계가 조금 달라도 그날 안에 지워지지 않게 여유를 둔다.
     */
    public boolean firstVisit(Long blogId, LocalDate date, String visitorKey) {
        Duration untilTomorrow = Duration.between(serviceDay.now(), serviceDay.startOf(date.plusDays(1)));
        Duration ttl = untilTomorrow.isNegative() ? visitMargin : untilTomorrow.plus(visitMargin);
        return mark("stats:visit:" + blogId + ":" + date + ":" + visitorKey, ttl);
    }

    private boolean mark(String key, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", ttl));
    }
}
