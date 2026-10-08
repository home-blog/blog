package com.myblog.blog.service;

import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.user.MemberRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 가입하면 내 블로그와 "미분류" 분류가 함께 생긴다 (001 FR-011, 003 FR-002·003).
 * 회원 모듈의 가입 이벤트를 받아, 가입과 같은 트랜잭션 안에서 바로 만든다 (실패하면 가입도 취소).
 */
@Component
public class BlogProvisioner {

    private final BlogRepository blogs;
    private final CategoryRepository categories;

    public BlogProvisioner(BlogRepository blogs, CategoryRepository categories) {
        this.blogs = blogs;
        this.categories = categories;
    }

    @EventListener
    public void on(MemberRegisteredEvent event) {
        Blog blog = blogs.save(Blog.createFor(event.memberId(), event.nickname()));
        categories.save(Category.defaultFor(blog.getId()));
    }
}
