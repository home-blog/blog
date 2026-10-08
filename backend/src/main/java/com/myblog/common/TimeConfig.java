package com.myblog.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 지금 시각은 Clock으로 받는다. 테스트에서 시각을 바꿔 끼울 수 있다. */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
