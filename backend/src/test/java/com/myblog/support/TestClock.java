package com.myblog.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트에서 지금 시각을 정하는 시계 (specs/006: 한국 날짜 23:59, 0:01 같은 경계를 만든다).
 * {@code @Import(TestClock.Config.class)}로 넣으면 서버의 Clock 대신 쓰인다. 정하지 않으면 실제 지금 시각.
 */
public class TestClock extends Clock {

    private volatile Instant fixed;

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
