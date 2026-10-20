package com.myblog.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트에서 지금 시각을 정하는 시계 (specs/006: 한국 날짜 23:59, 0:01 같은 경계를 만든다).
 * {@code @Import(TestClock.Config.class)}로 넣으면 서버의 Clock 대신 쓰인다. 정하지 않으면 실제 지금 시각.
 */
public class TestClock extends Clock {

    private volatile Instant fixed;

    /** 먼 미래 칸의 시작 (JVM마다 무작위로 한 번). 다른 실행과 겹치지 않게 */
    private static final Instant FAR_FUTURE_BASE = Instant.parse("2040-01-01T00:00:00Z")
            .plus(Duration.ofMinutes(10L * ThreadLocalRandom.current().nextLong(1, 400_000)));
    private static final AtomicLong NEXT_SLOT = new AtomicLong();
    /** 한 테스트가 쓰는 칸 수: 1시간 창 두 개(지금·10분 전)와 테스트 안에서 시계를 미는 몫(최대 75분)보다 넉넉히 */
    private static final long SLOTS_PER_TEST = 50;

    /**
     * 부를 때마다 겹치지 않는 먼 미래의 10분 칸 시작. 인기 검색어처럼 Redis에 실제 시간으로 남는 칸을 쓰는 테스트가
     * 같은 실행 안에서 서로의 값을 보지 않게 한다 (무작위로 고르면 가까운 칸이 뽑혀 섞일 수 있었다).
     */
    public static Instant distinctFarFutureSlot() {
        return FAR_FUTURE_BASE.plus(Duration.ofMinutes(10L * SLOTS_PER_TEST * NEXT_SLOT.getAndIncrement()));
    }

    public void set(Instant instant) {
        this.fixed = instant;
    }

    public void reset() {
        this.fixed = null;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        Instant now = fixed;
        return now != null ? now : Instant.now();
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }
}
