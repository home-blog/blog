package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.common.Visibility;
import com.myblog.post.PostContentSavedEvent;
import com.myblog.post.PostPublishedEvent;
import com.myblog.post.repository.PostRepository;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 글이 처음 공개되는 순간을 알린다 (specs/016 D-2, S2-2). 글을 저장하면(새 글·수정·임시 저장 글 발행 모두 PostContentSavedEvent)
 * 같은 트랜잭션 안에서, 그 글이 공개 글이고 아직 공개 글이었던 적이 없으면(first_published_at이 빔) 시각을 적고,
 * 그때 분류도 공개면 PostPublishedEvent를 낸다. 시각은 "비어 있을 때만 채우기"로 적으므로 같은 글을 두 번 알리지 않는다.
 * 비공개 분류에 올린 공개 글은 시각만 적혀, 나중에 분류를 공개로 바꾸거나 글을 고쳐도 알리지 않는다.
 */
@Component
public class PostPublicationMarker {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PostPublicationMarker(PostRepository posts, BlogDirectory blogDirectory, JdbcTemplate jdbc,
            ApplicationEventPublisher events, Clock clock) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
    }

    @EventListener
    public void on(PostContentSavedEvent event) {
        posts.findById(event.postId())
                .filter(post -> Visibility.PUBLIC.value().equals(post.getVisibility()))
                .ifPresent(post -> {
                    // 글이 처음 공개 글이 된 순간을 분류와 상관없이 적는다: 비공개 분류에 있다가 분류가 공개로 바뀐 뒤
                    // 고쳐 저장해도(바뀐 것 없이 저장해도) 옛 글을 새 원고로 알리지 않는다
                    int marked = jdbc.update("update post set first_published_at = ? where post_id = ?"
                            + " and first_published_at is null", Timestamp.from(clock.instant()), post.getId());
                    if (marked == 1) {
                        blogDirectory.category(post.getCategoryId())
                                .filter(category -> Visibility.PUBLIC.value().equals(category.visibility()))
                                .ifPresent(category -> events.publishEvent(new PostPublishedEvent(post.getId(),
                                        category.blogId(), category.ownerId(), category.blogName(), post.getTitle(),
                                        post.getContent())));
                    }
                });
    }
}
