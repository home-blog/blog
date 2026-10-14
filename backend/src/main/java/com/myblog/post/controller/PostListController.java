package com.myblog.post.controller;

import com.myblog.post.controller.dto.ExploreResponses.PostListResponse;
import com.myblog.post.service.PostListService;
import com.myblog.post.service.PostListService.ManuscriptTotal;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 한 블로그의 글 목록 (specs/004 contracts 1). 로그인 없이 부른다 (SecurityConfig의 GET /api/v1/blogs/**).
 * 주인인지는 세션의 회원(LoggedInMember)으로만 정한다. 요청에 "비공개도 보여 줘" 같은 값을 받지 않는다 (research B-1).
 * 페이지·분류 번호는 글자로 받아 서비스가 읽는다: 숫자가 아닌 페이지는 1페이지, 숫자가 아닌 분류는 없는 분류다 (004 D-2).
 */
@Tag(name = "6. 둘러보기·검색")
@RestController
public class PostListController {

    private final LoggedInMember loggedInMember;
    private final PostListService listService;

    public PostListController(LoggedInMember loggedInMember, PostListService listService) {
        this.loggedInMember = loggedInMember;
        this.listService = listService;
    }

    @Operation(summary = "한 블로그의 글 목록", description = "방문자는 공개 글, 주인은 비공개 글까지. categoryId로 분류별 목록.")
    @GetMapping("/api/{version}/blogs/{blogId}/posts")
    public PostListResponse list(@PathVariable Long blogId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String categoryId, Authentication authentication) {
        return PostListResponse.of(
                listService.list(blogId, loggedInMember.idOf(authentication).orElse(null), page, categoryId));
    }

    /** 블로그 머리의 "지금까지 쓴 원고지" (v0.5.1). 보이는 글만 센다: 방문자는 공개 글, 주인은 비공개까지. */
    @Operation(summary = "블로그가 지금까지 쓴 원고지 매수")
    @GetMapping("/api/{version}/blogs/{blogId}/manuscript")
    public ManuscriptTotal manuscript(@PathVariable Long blogId, Authentication authentication) {
        return listService.manuscriptTotal(blogId, loggedInMember.idOf(authentication).orElse(null));
    }
}
