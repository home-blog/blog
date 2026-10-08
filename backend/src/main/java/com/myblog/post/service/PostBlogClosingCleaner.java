package com.myblog.post.service;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.post.PostDeletingEvent;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원이 탈퇴해 블로그가 닫히면 그 블로그의 글을 모두 지운다 (specs/003 T036, 002 FR-024, 002 T035의 약속).
 * 블로그 모듈이 분류를 지우기 <b>전에</b> 낸 BlogClosingEvent를 같은 트랜잭션 안에서 듣는다.
 * 글마다 PostDeletingEvent를 내서 딸린 것(댓글 등, 005)도 먼저 지우게 한 뒤 글을 지운다.
 */
@Component
public class PostBlogClosingCleaner {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final ApplicationEventPublisher events;

    public PostBlogClosingCleaner(PostRepository posts, BlogDirectory blogDirectory, ApplicationEventPublisher events) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.events = events;
    }

    @EventListener
    public void on(BlogClosingEvent event) {
        List<Long> categoryIds = blogDirectory.categoriesOf(event.blogId()).stream().map(CategoryInfo::categoryId).toList();
        if (categoryIds.isEmpty()) {
            return;
        }
        List<Post> list = posts.findByCategoryIdIn(categoryIds);
        list.forEach(post -> events.publishEvent(new PostDeletingEvent(post.getId())));
        posts.deleteAll(list);
        posts.flush();
    }
}
