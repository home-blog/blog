package com.myblog.post.controller;

import com.myblog.post.controller.dto.ExploreResponses.PostSearchResponse;
import com.myblog.post.service.PostSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 글 검색 (specs/004 contracts 2). 로그인 없이 부르고(SecurityConfig의 GET /api/search/**), 로그인해도 결과가 같다.
 * 검색어와 페이지 번호는 글자 그대로 받아 서비스가 다듬고 검사한다.
 */
@RestController
public class PostSearchController {

    private final PostSearchService searchService;

    public PostSearchController(PostSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search/posts")
    public PostSearchResponse search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String page) {
        return PostSearchResponse.of(searchService.search(q, page));
    }
}
