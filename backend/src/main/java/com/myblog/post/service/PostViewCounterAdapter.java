package com.myblog.post.service;

import com.myblog.post.PostViewCounter;
import com.myblog.post.repository.PostRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostViewCounter를 글 표로 채운다 (specs/006 T051). 부르는 쪽의 트랜잭션에 함께 든다. */
@Component
public class PostViewCounterAdapter implements PostViewCounter {

    private final PostRepository posts;

    public PostViewCounterAdapter(PostRepository posts) {
        this.posts = posts;
    }

    @Override
    @Transactional
    public void increment(Long postId) {
        posts.incrementViews(postId);
    }
}
