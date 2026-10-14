package com.myblog.post.controller;

import com.myblog.post.controller.dto.ExploreResponses.PostSearchResponse;
import com.myblog.post.service.PostSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 글 검색 (specs/004 contracts 2). 로그인 없이 부르고(SecurityConfig의 GET /api/v1/search/**), 로그인해도 결과가 같다.
 * 검색어와 페이지 번호는 글자 그대로 받아 서비스가 다듬고 검사한다.
 */
@Tag(name = "6. 둘러보기·검색")
@RestController
public class PostSearchController {

    private final PostSearchService searchService;

    public PostSearchController(PostSearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "글 검색", description = "공개 글의 제목·본문에서 찾는다.")
    @GetMapping("/api/{version}/search/posts")
    public PostSearchResponse search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String page) {
        return PostSearchResponse.of(searchService.search(q, page));
    }
}
