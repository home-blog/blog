package com.myblog.user.config;

import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 회원 정지의 숫자 (application.yml의 moderation.member.*, specs/022 US2).
 * suspendDays: 편집실이 고를 수 있는 정지 기간(일), zone: 로그인 화면에 "언제까지"를 보여 줄 시간대.
 */
@ConfigurationProperties(prefix = "moderation.member")
public record MemberModerationProperties(List<Integer> suspendDays, ZoneId zone) {

    public MemberModerationProperties {
        if (suspendDays == null || suspendDays.isEmpty() || suspendDays.stream().anyMatch(d -> d == null || d < 1)) {
            throw new IllegalStateException("moderation.member.suspend-days는 1 이상의 일 수가 하나 이상 있어야 합니다");
        }
        if (zone == null) {
            throw new IllegalStateException("moderation.member.zone이 필요합니다");
        }
        suspendDays = List.copyOf(suspendDays);
    }
}
