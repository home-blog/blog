package com.myblog.blog.controller;

import com.myblog.blog.controller.dto.BlogRequests;
import com.myblog.blog.service.BlogQueryService;
import com.myblog.blog.service.BlogQueryService.BlogView;
import com.myblog.blog.service.BlogQueryService.CategoryView;
import com.myblog.blog.service.BlogSettingsService;
import com.myblog.user.LoggedInMember;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블로그 보기 (specs/003 contracts 1 ~ 3).
 * /api/blogs/**는 누구나 읽는다 (SecurityConfig). /api/me/blog는 로그인해야 하고, 내 블로그는 세션의 회원으로만 정한다 (FR-006).
 * 회원 번호는 LoggedInMember로만 얻는다 (주소·본문에서 받지 않음).
 */
@RestController
public class BlogController {

    private final LoggedInMember loggedInMember;
    private final BlogQueryService queryService;
    private final BlogSettingsService settingsService;

    public BlogController(LoggedInMember loggedInMember, BlogQueryService queryService, BlogSettingsService settingsService) {
        this.loggedInMember = loggedInMember;
        this.queryService = queryService;
        this.settingsService = settingsService;
    }

    /** 내 블로그 (contracts 3). */
    @GetMapping("/api/me/blog")
    public MyBlogResponse myBlog(Authentication authentication) {
        BlogView blog = queryService.myBlog(loggedInMember.requireIdOf(authentication));
        return new MyBlogResponse(blog.blogId(), blog.name(), blog.intro());
    }

    /** 내 블로그 이름·소개 고치기 (contracts 4). 블로그 삭제 주소는 없다 (FR-007). */
    @PutMapping("/api/me/blog")
    public MyBlogResponse changeProfile(Authentication authentication, @Valid @RequestBody BlogRequests.UpdateProfile request) {
        BlogView blog = settingsService.changeProfile(loggedInMember.requireIdOf(authentication), request.name(), request.intro());
        return new MyBlogResponse(blog.blogId(), blog.name(), blog.intro());
    }

    /** 블로그 정보 (contracts 1). */
    @GetMapping("/api/blogs/{blogId}")
    public BlogView blog(@PathVariable Long blogId, Authentication authentication) {
        return queryService.blog(blogId, loggedInMember.idOf(authentication).orElse(null));
    }

    /** 분류 목록과 글 개수 (contracts 2). 보는 사람에 따라 비공개 분류와 개수가 다르다. */
    @GetMapping("/api/blogs/{blogId}/categories")
    public CategoriesResponse categories(@PathVariable Long blogId, Authentication authentication) {
        return new CategoriesResponse(queryService.categories(blogId, loggedInMember.idOf(authentication).orElse(null)));
    }

    public record MyBlogResponse(Long blogId, String name, String intro) {
    }

    public record CategoriesResponse(List<CategoryView> categories) {
    }
}
