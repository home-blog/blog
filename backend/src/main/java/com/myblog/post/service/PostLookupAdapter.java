package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.post.PostLookup;
import com.myblog.post.repository.PostRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostLookup을 글 표, BlogDirectory(글 → 분류 → 블로그 → 주인), PostVisibility로 채운다 (specs/005 T009). */
@Component
@Transactional(readOnly = true)
public class PostLookupAdapter implements PostLookup {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;

    public PostLookupAdapter(PostRepository posts, BlogDirectory blogDirectory, PostVisibility visibility) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
    }

    @Override
    public Optional<PostRef> findVisible(Long postId, Long viewerId) {
        return posts.findById(postId).flatMap(post -> blogDirectory.category(post.getCategoryId())
                .filter(category -> visibility.canSee(post.getVisibility(), category, viewerId))
                .map(category -> new PostRef(post.getId(), category.blogId(), category.ownerId())));
    }
}
