package com.myblog.post.controller;

import com.myblog.post.service.BlogActivityService;
import com.myblog.post.service.BlogActivityService.Activity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 글쓰기 잔디 (specs/013 US1, contracts 1). 로그인 없이 부른다 (SecurityConfig의 GET /api/v1/blogs/**). */
@Tag(name = "12. 글쓰기 도구")
@RestController
public class BlogActivityController {

    private final BlogActivityService activityService;

    public BlogActivityController(BlogActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(summary = "글쓰기 잔디 (최근 1년)",
            description = "오늘 포함 `writing.activity.days`일 동안 공개 분류의 공개 글을 쓴 날별 글 수와 원고지 매수. "
                    + "날짜는 `writing.activity.zone`(한국 시간)으로 센다. 글이 있는 날만 담는다. 주인이 봐도 같다.")
    @GetMapping("/api/{version}/blogs/{blogId}/activity")
    public Activity activity(@PathVariable Long blogId) {
        return activityService.activity(blogId);
    }
}
