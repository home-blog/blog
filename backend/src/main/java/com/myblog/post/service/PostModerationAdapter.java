package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.post.PostModeration;
import com.myblog.post.repository.PostRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostModeration을 글 표로 채운다 (specs/018 D-2). 부르는 쪽의 트랜잭션에 함께 든다. */
@Component
public class PostModerationAdapter implements PostModeration {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostPreview preview;

    public PostModerationAdapter(PostRepository posts, BlogDirectory blogDirectory, PostPreview preview) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.preview = preview;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostBrief> brief(Long postId) {
        return posts.findById(postId).flatMap(post -> blogDirectory.category(post.getCategoryId())
                .map(category -> new PostBrief(post.getId(), category.blogId(), category.ownerId(), category.blogName(),
                        post.getTitle(), preview.of(post.getContent()), post.getVisibility(), post.isLocked(),
                        post.getLockReason())));
    }

    @Override
    @Transactional
    public void lock(Long postId, String reason, Instant now) {
        posts.lock(postId, reason, now);
    }

    @Override
    @Transactional
    public boolean unlock(Long postId) {
        return posts.unlock(postId) > 0;
    }
}
