package com.myblog.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 블로그 관리 화면의 숫자 (application.yml의 manage.*, specs/006 plan.md 설정값 목록, 헌법 VI).
 * 글 관리(post), 댓글 관리(comment), 대시보드·통계(stats)가 함께 쓰므로 열린 모듈 common에 둔다. 잘못 정하면 서버가 켜지지 않는다.
 */
@ConfigurationProperties(prefix = "manage")
public record ManageProperties(int pageSize, Comment comment, Dashboard dashboard, Stats stats) {

    public ManageProperties {
        if (pageSize < 1) {
            throw new IllegalStateException("manage.page-size는 1 이상이어야 합니다");
        }
    }

    /** 댓글 관리 목록의 앞부분 글자 수 (코드 포인트, FR-024). */
    public record Comment(int previewLength) {

        public Comment {
            if (previewLength < 1) {
                throw new IllegalStateException("manage.comment.preview-length는 1 이상이어야 합니다");
            }
        }
    }

    /** 대시보드: 인기 글은 최근 popularDays일 조회수로 popularSize개, 최근 글 recentSize개, 그래프 chartDays일 (FR-007 ~ FR-009). */
    public record Dashboard(int popularDays, int popularSize, int recentSize, int chartDays) {

        public Dashboard {
            if (popularDays < 1 || popularSize < 1 || recentSize < 1 || chartDays < 1) {
                throw new IllegalStateException("manage.dashboard의 값은 모두 1 이상이어야 합니다");
            }
        }
    }

    /** 통계 화면의 기간 (7일, 30일). 기본 기간은 고를 수 있는 기간 중 하나여야 한다 (FR-031). */
    public record Stats(List<Integer> periodOptions, int periodDefault) {

        public Stats {
            if (periodOptions == null || periodOptions.isEmpty() || periodOptions.stream().anyMatch(days -> days == null || days < 1)) {
                throw new IllegalStateException("manage.stats.period-options는 1 이상의 날 수를 하나 이상 담아야 합니다");
            }
            if (!periodOptions.contains(periodDefault)) {
                throw new IllegalStateException("manage.stats.period-default는 period-options 중 하나여야 합니다");
            }
            periodOptions = List.copyOf(periodOptions);
        }
    }
}
