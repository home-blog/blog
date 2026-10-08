package com.myblog.blog.service;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.user.MemberWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 탈퇴한 회원의 블로그를 지운다 (specs/002 T032, FR-024, data-model 3의 9·11번).
 * 탈퇴 트랜잭션 안에서 바로 돈다: ① 블로그를 찾고 ② BlogClosingEvent로 아래 모듈(글, 댓글, 통계)이 먼저 지우게 하고
 * ③ 분류를 모두(미분류 포함) ④ 블로그를 지운다. 하나라도 실패하면 탈퇴 전체가 취소된다.
 */
@Component
public class BlogWithdrawalCleaner {

    private final BlogRepository blogs;
    private final CategoryRepository categories;
    private final ApplicationEventPublisher events;

    public BlogWithdrawalCleaner(BlogRepository blogs, CategoryRepository categories, ApplicationEventPublisher events) {
        this.blogs = blogs;
        this.categories = categories;
        this.events = events;
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        blogs.findByOwnerId(event.memberId()).ifPresent(blog -> {
            events.publishEvent(new BlogClosingEvent(blog.getId(), event.memberId()));
            categories.deleteByBlogId(blog.getId());
            blogs.delete(blog);
            blogs.flush();
        });
    }
}
