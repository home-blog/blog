package com.myblog.blog.service;

import com.myblog.blog.domain.Blog;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.user.MemberBlogLookup;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 회원 모듈의 MemberBlogLookup을 블로그 표로 채운다 (마이페이지의 "내 블로그" 바로가기, specs/002 T012). */
@Component
public class MemberBlogLookupAdapter implements MemberBlogLookup {

    private final BlogRepository blogs;

    public MemberBlogLookupAdapter(BlogRepository blogs) {
        this.blogs = blogs;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> blogIdOf(Long memberId) {
        return blogs.findByOwnerId(memberId).map(Blog::getId);
    }
}
