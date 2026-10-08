package com.myblog.stats.time;

import com.myblog.stats.config.StatsProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 한국 날짜 계산 한곳 (specs/006 T006, research B-4, R-1, FR-036, SC-007).
 * 지금 시각은 Clock(UTC)으로, 날짜는 stats.day-zone으로만 정한다. 서버·DB의 시간대 설정에 기대지 않는다.
 */
@Component
public class ServiceDay {

    private final Clock clock;
    private final ZoneId zone;

    public ServiceDay(Clock clock, StatsProperties properties) {
        this.clock = clock;
        this.zone = properties.dayZone();
    }

    public ZoneId zone() {
        return zone;
    }

    public Instant now() {
        return Instant.now(clock);
    }

    /** 오늘 (한국 날짜). */
    public LocalDate today() {
        return dateOf(now());
    }

    /** 이 순간의 한국 날짜. */
    public LocalDate dateOf(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    /** 이 한국 날짜가 시작하는 순간 (그날 0시). */
    public Instant startOf(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    /** 오늘을 포함한 n일, 오래된 날부터. n은 1 이상 (기간은 설정값에서 온다). */
    public List<LocalDate> lastDays(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("days는 1 이상이어야 합니다");
        }
        LocalDate today = today();
        return Stream.iterate(today.minusDays(days - 1L), date -> date.plusDays(1)).limit(days).toList();
    }
}
