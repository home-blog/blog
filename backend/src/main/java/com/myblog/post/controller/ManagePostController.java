package com.myblog.post.controller;

import com.myblog.post.service.ManagePostQueryService;
import com.myblog.post.service.ManagePostQueryService.ManagePostPage;
import com.myblog.user.LoggedInMember;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블로그 관리의 글 목록 (specs/006 contracts 3-1, T020). 로그인해야 하고, 내 블로그는 세션으로만 정한다.
 * 쪽 번호는 1부터. 1보다 작거나 숫자가 아니면 400 VALIDATION_FAILED (FR-014, NF-07).
 * 아주 큰 쪽 번호는 건너뛸 줄 수가 int를 넘지 않게 막는다 (그런 쪽은 어차피 비어 있다).
 */
@RestController
public class ManagePostController {

    private final LoggedInMember loggedInMember;
    private final ManagePostQueryService queryService;

    public ManagePostController(LoggedInMember loggedInMember, ManagePostQueryService queryService) {
        this.loggedInMember = loggedInMember;
        this.queryService = queryService;
    }

    @GetMapping("/api/manage/posts")
    public ManagePostPage list(Authentication authentication,
            @RequestParam(defaultValue = "all") String visibility,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") @Min(1) @Max(Integer.MAX_VALUE / 100) int page) {
        return queryService.list(loggedInMember.requireIdOf(authentication), visibility, categoryId, page);
    }
}
