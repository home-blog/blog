package com.myblog.post.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 글 목록·검색의 숫자 (application.yml의 explore.*, specs/004 plan.md 설정값 목록, 헌법 VI).
 * 잘못 정하면 서버가 켜지지 않는다. 글자는 코드 포인트로 센다.
 * 검색 결과도 글 목록과 같은 페이지 글 수·미리보기 길이를 쓴다 (004 D-3: A, 같은 설정값).
 */
@ConfigurationProperties(prefix = "explore")
public record ExploreProperties(ListPage list, Search search, Feed feed, TodayIssue todayIssue) {

    /** 한 페이지 글 수(10)와 미리보기 길이(100자). */
    public record ListPage(int pageSize, int previewLength) {

        public ListPage {
            if (pageSize < 1 || previewLength < 1) {
                throw new IllegalStateException("explore.list의 page-size와 preview-length는 1 이상이어야 합니다");
            }
        }
    }

    /** 검색어 길이 2~50자 (앞뒤 공백을 지운 뒤, 004 FR-010, D-1). */
    public record Search(int keywordMinLength, int keywordMaxLength) {

        public Search {
            if (keywordMinLength < 1 || keywordMaxLength < keywordMinLength) {
                throw new IllegalStateException("explore.search는 1 <= keyword-min-length <= keyword-max-length이어야 합니다");
            }
        }
    }

    /**
     * 첫 화면 글 모음: 한 페이지 글 수(12), 인기 글 수(5)와 기간(최근 7일, 오늘 포함), 날짜를 세는 시간대(한국).
     * 인기 글은 그 기간의 글별 일별 조회수(post_daily_stat) 합으로 고른다.
     * summaryTtl은 첫 화면 머리 분량(블로거·글·매수)을 다시 세기까지 기다리는 시간이다 (v0.5.1). 0이면 매번 센다.
     */
    public record Feed(int pageSize, int popularSize, int popularDays, ZoneId dayZone, Duration summaryTtl) {

        public Feed {
            if (pageSize < 1 || popularSize < 1 || popularDays < 1 || dayZone == null) {
                throw new IllegalStateException(
                        "explore.feed의 page-size, popular-size, popular-days는 1 이상이고 day-zone이 있어야 합니다");
            }
            if (summaryTtl == null || summaryTtl.isNegative()) {
                throw new IllegalStateException("explore.feed.summary-ttl은 0 이상이어야 합니다");
            }
        }
    }

    /**
     * 오늘의 이슈 (specs/015 US2): 오늘을 포함한 최근 days일(2 = 오늘과 어제, 한국 날짜)의 조회수 합이 큰 공개 글 size개,
     * 블로그마다 하나. 고른 순서는 ttl 동안 다시 쓴다(첫 화면마다 부른다). 카드는 요청마다 다시 읽어 볼 수 없게 된 글은 바로 빠진다.
     */
    public record TodayIssue(int size, int days, Duration ttl) {

        public TodayIssue {
            if (size < 1 || days < 1 || ttl == null || ttl.isNegative()) {
                throw new IllegalStateException("explore.today-issue의 size, days는 1 이상, ttl은 0 이상이어야 합니다");
            }
        }
    }
}
