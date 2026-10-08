package com.myblog.blog.service;

import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.user.domain.User;
import com.myblog.user.service.MemberCreatedHandler;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** 가입하면 내 블로그와 "미분류" 분류가 함께 생긴다 (001 FR-011, 003 FR-002·003). 가입과 같은 트랜잭션에서 돈다. */
@Component
public class BlogProvisioner implements MemberCreatedHandler {

    private final BlogRepository blogs;
    private final CategoryRepository categories;
    private final Clock clock;

    public BlogProvisioner(BlogRepository blogs, CategoryRepository categories, Clock clock) {
        this.blogs = blogs;
        this.categories = categories;
        this.clock = clock;
    }

    @Override
    public void onMemberCreated(User member) {
        Blog blog = blogs.save(Blog.createFor(member.getId(), member.getNickname()));
        categories.save(Category.defaultFor(blog.getId(), Instant.now(clock)));
    }
}
