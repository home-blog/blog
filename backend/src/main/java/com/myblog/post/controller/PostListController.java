package com.myblog.post.controller;

import com.myblog.post.controller.dto.ExploreResponses.PostListResponse;
import com.myblog.post.service.PostListService;
import com.myblog.user.LoggedInMember;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 한 블로그의 글 목록 (specs/004 contracts 1). 로그인 없이 부른다 (SecurityConfig의 GET /api/blogs/**).
 * 주인인지는 세션의 회원(LoggedInMember)으로만 정한다. 요청에 "비공개도 보여 줘" 같은 값을 받지 않는다 (research B-1).
 * 페이지 번호는 글자로 받아 서비스가 읽는다: 숫자가 아니어도 오류가 아니다 (004 D-2).
 */
@RestController
public class PostListController {

    private final LoggedInMember loggedInMember;
    private final PostListService listService;

    public PostListController(LoggedInMember loggedInMember, PostListService listService) {
        this.loggedInMember = loggedInMember;
        this.listService = listService;
    }

    @GetMapping("/api/blogs/{blogId}/posts")
    public PostListResponse list(@PathVariable Long blogId, @RequestParam(required = false) String page,
            Authentication authentication) {
        return PostListResponse.of(listService.list(blogId, loggedInMember.idOf(authentication).orElse(null), page));
    }
}
