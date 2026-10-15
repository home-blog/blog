package com.myblog.post.controller;

import com.myblog.post.service.TagPostService;
import com.myblog.post.service.TagPostService.TagPostsView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 같은 태그의 공개 글 목록 (specs/005 contracts 9). 로그인 없이 부르고(SecurityConfig의 GET /api/v1/tags/**), 누가 봐도 같다. */
@Tag(name = "6. 둘러보기·검색")
@RestController
public class TagPostController {

    private final TagPostService tagPostService;

    public TagPostController(TagPostService tagPostService) {
        this.tagPostService = tagPostService;
    }

    @Operation(summary = "같은 태그의 글")
    @GetMapping("/api/{version}/tags/{tagName}/posts")
    public TagPostsView list(@PathVariable String tagName, @RequestParam(required = false) String page) {
        return tagPostService.list(tagName, page);
    }
}
