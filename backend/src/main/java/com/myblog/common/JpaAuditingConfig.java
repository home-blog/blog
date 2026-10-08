package com.myblog.common;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Spring Data JPA Auditing: @CreatedDate, @LastModifiedDate 칸을 저장할 때 자동으로 채운다.
 * 시각은 Clock 빈에서 가져온다. 처음 만들 때는 고친 시각을 채우지 않는다 (아직 고친 적 없음).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider", modifyOnCreate = false)
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }
}
