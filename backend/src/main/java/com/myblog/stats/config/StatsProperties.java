package com.myblog.stats.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 조회수·방문자 세기의 숫자 (application.yml의 stats.*, specs/006 plan.md 설정값 목록, 헌법 VI).
 * <ul>
 *   <li>dayZone: "하루"를 나누는 시간대 (한국 시간, FR-036). 서버·DB의 시간대 설정에 기대지 않는다.</li>
 *   <li>view.dedupeWindow: 같은 사람이 같은 글을 이 시간 안에 다시 열면 조회수를 세지 않는다 (30분, FR-033).</li>
 *   <li>visitor.cookieMaxAge: 비회원 방문자 쿠키 유지 기간 (1년, 가안, D-2).</li>
 *   <li>visitor.visitMargin: "오늘 왔다" 표시를 다음 한국 자정보다 이만큼 더 둔다 (시계 차이 여유, 1시간).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "stats")
public record StatsProperties(ZoneId dayZone, View view, Visitor visitor) {

    public StatsProperties {
        if (dayZone == null) {
            throw new IllegalStateException("stats.day-zone을 정해야 합니다");
        }
    }

    public record View(Duration dedupeWindow) {

        public View {
            if (dedupeWindow == null || dedupeWindow.isNegative() || dedupeWindow.isZero()) {
                throw new IllegalStateException("stats.view.dedupe-window는 0보다 커야 합니다");
            }
        }
    }

    /** cookieName은 영문·숫자·밑줄·하이픈만. */
    public record Visitor(String cookieName, Duration cookieMaxAge, Duration visitMargin) {

        public Visitor {
            if (cookieName == null || !cookieName.matches("[A-Za-z0-9_-]+")) {
                throw new IllegalStateException("stats.visitor.cookie-name은 영문·숫자·밑줄·하이픈이어야 합니다");
            }
            if (cookieMaxAge == null || cookieMaxAge.isNegative() || cookieMaxAge.isZero()) {
                throw new IllegalStateException("stats.visitor.cookie-max-age는 0보다 커야 합니다");
            }
            if (visitMargin == null || visitMargin.isNegative() || visitMargin.isZero()) {
                throw new IllegalStateException("stats.visitor.visit-margin은 0보다 커야 합니다");
            }
        }
    }
}
