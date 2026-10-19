package com.myblog.subscription.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 구독·알림의 숫자 (application.yml의 subscription.*, specs/016).
 * <ul>
 *   <li>maxPerMember: 한 사람이 받아 볼 수 있는 블로그 수(500)</li>
 *   <li>notificationRetention: 우편함 소식 보관(30일), notificationPageSize: 우편함 한 쪽(20)</li>
 *   <li>newsletter: 원고 편지(메일) 켜기, 하루 한도(400), 메일 속 링크의 사이트 주소, 요약 길이(120자)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "subscription")
public record SubscriptionProperties(int maxPerMember, Duration notificationRetention, int notificationPageSize,
        Newsletter newsletter) {

    public SubscriptionProperties {
        if (maxPerMember < 1 || notificationPageSize < 1) {
            throw new IllegalStateException("subscription.max-per-member와 notification-page-size는 1 이상이어야 합니다");
        }
        if (notificationRetention == null || notificationRetention.toDays() < 1) {
            throw new IllegalStateException("subscription.notification-retention은 1일 이상이어야 합니다");
        }
        if (newsletter == null) {
            throw new IllegalStateException("subscription.newsletter 설정이 필요합니다");
        }
    }

    /** siteUrl은 끝에 /가 없는 사이트 주소(메일 속 글 링크·그만 받기 링크). 비어 있으면 메일을 보내지 않는다. */
    public record Newsletter(boolean enabled, int dailyLimit, String siteUrl, int summaryLength) {

        public Newsletter {
            if (dailyLimit < 0 || summaryLength < 1) {
                throw new IllegalStateException("subscription.newsletter.daily-limit은 0 이상, summary-length는 1 이상이어야 합니다");
            }
            siteUrl = siteUrl == null ? "" : siteUrl.strip().replaceAll("/+$", "");
        }

        public boolean canSend() {
            return enabled && dailyLimit > 0 && !siteUrl.isEmpty();
        }
    }
}
