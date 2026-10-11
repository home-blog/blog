package com.myblog.post.controller;

import com.myblog.post.service.PostFeedService;
import com.myblog.post.service.PostFeedService.FeedView;
import com.myblog.post.service.PostFeedService.PopularView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 첫 화면 글 모음: GET /api/posts?topic=여행&amp;page=2, 이번 주 인기 글: GET /api/posts/popular.
 * 로그인 없이 부른다 (SecurityConfig의 GET /api/posts, /api/posts/popular).
 * 주제와 페이지 번호는 글자 그대로 받아 서비스가 다듬고 검사한다.
 */
@RestController
public class PostFeedController {

    private final PostFeedService feedService;

    public PostFeedController(PostFeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/api/posts")
    public FeedView feed(@RequestParam(required = false) String topic, @RequestParam(required = false) String page) {
        return feedService.feed(topic, page);
    }

    /** 이번 주 인기 글. GET /api/posts/popular?topic=여행 */
    @GetMapping("/api/posts/popular")
    public PopularView popular(@RequestParam(required = false) String topic) {
        return feedService.popular(topic);
    }
}
