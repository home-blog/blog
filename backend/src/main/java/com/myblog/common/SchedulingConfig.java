package com.myblog.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 정해진 간격으로 도는 작업(@Scheduled)을 켠다. 예: 이미지 정리 (specs/005 T058). 여기서 한 번만 켠다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
