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
 * 같은 트랜잭션 안에서, 그 글이 공개 분류의 공개 글이고 아직 공개된 적이 없으면(first_published_at이 빔) 시각을 적고 PostPublishedEvent를 낸다.
 * 시각은 "비어 있을 때만 채우기"로 적으므로 같은 글을 두 번 알리지 않는다. 분류를 공개로 바꿔 한꺼번에 보이게 된 글은 알리지 않는다.
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
                .ifPresent(post -> blogDirectory.category(post.getCategoryId())
                        .filter(category -> Visibility.PUBLIC.value().equals(category.visibility()))
                        .ifPresent(category -> {
                            int marked = jdbc.update("update post set first_published_at = ? where post_id = ?"
                                    + " and first_published_at is null", Timestamp.from(clock.instant()), post.getId());
                            if (marked == 1) {
                                events.publishEvent(new PostPublishedEvent(post.getId(), category.blogId(),
                                        category.ownerId(), category.blogName(), post.getTitle(), post.getContent()));
                            }
                        }));
    }
}
