package com.myblog.post.controller;

import com.myblog.post.service.PostFeedService;
import com.myblog.post.service.PostFeedService.FeedView;
import com.myblog.post.service.PostFeedService.PopularView;
import com.myblog.post.service.PostFeedService.SiteSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 첫 화면 글 모음: GET /api/v1/posts?topic=여행&amp;page=2, 이번 주 인기 글: GET /api/v1/posts/popular.
 * 로그인 없이 부른다 (SecurityConfig의 GET /api/v1/posts, /api/posts/popular).
 * 주제와 페이지 번호는 글자 그대로 받아 서비스가 다듬고 검사한다.
 */
@Tag(name = "6. 둘러보기·검색")
@RestController
public class PostFeedController {

    private final PostFeedService feedService;

    public PostFeedController(PostFeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(summary = "첫 화면 글 모음", description = "모든 블로그의 공개 글. topic(여행·음식·취미·운동·개발)으로 거른다.")
    @GetMapping("/api/{version}/posts")
    public FeedView feed(@RequestParam(required = false) String topic, @RequestParam(required = false) String page) {
        return feedService.feed(topic, page);
    }

    /** 첫 화면 머리의 분량 (v0.5.1). GET /api/v1/posts/summary */
    @Operation(summary = "첫 화면 분량 (블로거·글·원고지 매수)", description = "원고지 매수는 글마다 200자에 1매로 올림해 더한다. 5분 동안 캐시.")
    @GetMapping("/api/{version}/posts/summary")
    public SiteSummary summary() {
        return feedService.summary();
    }

    /** 이번 주 인기 글. GET /api/v1/posts/popular?topic=여행 */
    @Operation(summary = "이번 주 인기 글", description = "최근 7일 조회수 순.")
    @GetMapping("/api/{version}/posts/popular")
    public PopularView popular(@RequestParam(required = false) String topic) {
        return feedService.popular(topic);
    }
}
