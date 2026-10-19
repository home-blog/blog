package com.myblog.subscription.service;

import com.myblog.subscription.config.SubscriptionProperties;
import com.myblog.subscription.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 보관 기간(30일)이 지난 우편함 소식을 지운다 (specs/016 D-5). 두 벌이 함께 돌아도 같은 줄을 지울 뿐이라 잠그지 않는다. */
@Component
public class NotificationJanitor {

    private static final Logger log = LoggerFactory.getLogger(NotificationJanitor.class);

    private final NotificationRepository notifications;
    private final SubscriptionProperties properties;
    private final Clock clock;

    public NotificationJanitor(NotificationRepository notifications, SubscriptionProperties properties, Clock clock) {
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${subscription.cleanup-cron:0 0 4 * * *}", zone = "${stats.day-zone}")
    @Transactional
    public int clean() {
        int removed = notifications.deleteOlderThan(Instant.now(clock).minus(properties.notificationRetention()));
        if (removed > 0) {
            log.info("우편함: 보관 기간이 지난 소식 {}개를 지웠습니다", removed);
        }
        return removed;
    }
}
