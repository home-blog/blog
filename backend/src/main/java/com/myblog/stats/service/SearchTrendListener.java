package com.myblog.stats.service;

import com.myblog.post.PostSearchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결과가 있는 검색을 인기 검색어로 센다 (specs/015 US1). <b>로그인한 회원의 검색만</b> 센다: 방문자 쿠키는 아무 값이나 지어 보낼 수 있어
 * 쿠키마다 다른 사람으로 세면 로그인 없이 순위를 꾸밀 수 있다(PR #83 리뷰). 회원은 이메일 인증을 거쳐야 늘어난다.
 * 모든 예외를 삼킨다: 세기가 실패해도 검색 결과는 그대로 보인다.
 */
@Component
public class SearchTrendListener {

    private static final Logger log = LoggerFactory.getLogger(SearchTrendListener.class);

    private final SearchTrends trends;

    public SearchTrendListener(SearchTrends trends) {
        this.trends = trends;
    }

    @EventListener
    public void on(PostSearchedEvent event) {
        if (event.viewerId() == null) {
            return;
        }
        try {
            trends.record(event.keyword(), "m:" + event.viewerId());
        } catch (RuntimeException e) {
            log.warn("인기 검색어를 세지 못했습니다: {}", e.toString());
        }
    }
}
