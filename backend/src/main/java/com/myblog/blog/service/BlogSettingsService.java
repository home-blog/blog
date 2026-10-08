package com.myblog.blog.service;

import com.myblog.blog.domain.Blog;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.service.BlogQueryService.BlogView;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 블로그 이름·소개 고치기 (specs/003 contracts 4, FR-004 ~ FR-007). 내 블로그는 세션의 회원으로만 정한다.
 * 캐시가 없어서 저장하면 다음 요청부터 모든 화면에 새 값이 보인다 (SC-010). 블로그를 지우는 기능은 없다 (FR-007).
 */
@Service
public class BlogSettingsService {

    private final BlogRepository blogs;

    public BlogSettingsService(BlogRepository blogs) {
        this.blogs = blogs;
    }

    @Transactional
    public BlogView changeProfile(Long memberId, String name, String intro) {
        Blog blog = blogs.findByOwnerId(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        blog.changeProfile(name, intro);
        blogs.flush();
        return BlogView.of(blog, true);
    }
}
