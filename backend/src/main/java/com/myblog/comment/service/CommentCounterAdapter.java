package com.myblog.comment.service;

import com.myblog.comment.repository.CommentRepository;
import com.myblog.post.PostCommentCounter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 글 모듈의 PostCommentCounter를 채운다 → 글 상세의 commentCount (specs/005 T020). */
@Component
@Transactional(readOnly = true)
public class CommentCounterAdapter implements PostCommentCounter {

    private final CommentRepository comments;

    public CommentCounterAdapter(CommentRepository comments) {
        this.comments = comments;
    }

    @Override
    public long count(Long postId) {
        return comments.countByPostId(postId);
    }
}
