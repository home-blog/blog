package com.myblog.stats.service;

import com.myblog.post.PostSearchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결과가 있는 검색을 인기 검색어로 센다 (specs/015 US1). 사람은 조회수와 같은 방법(회원 번호, 없으면 방문자 쿠키)으로 가린다.
 * 모든 예외를 삼킨다: 세기가 실패해도 검색 결과는 그대로 보인다.
 */
@Component
public class SearchTrendListener {

    private static final Logger log = LoggerFactory.getLogger(SearchTrendListener.class);

    private final VisitorKeyResolver visitorKeys;
    private final SearchTrends trends;

    public SearchTrendListener(VisitorKeyResolver visitorKeys, SearchTrends trends) {
        this.visitorKeys = visitorKeys;
        this.trends = trends;
    }

    @EventListener
    public void on(PostSearchedEvent event) {
        try {
            visitorKeys.resolve(event.viewerId()).ifPresent(visitor -> trends.record(event.keyword(), visitor));
        } catch (RuntimeException e) {
            log.warn("인기 검색어를 세지 못했습니다: {}", e.toString());
        }
    }
}
