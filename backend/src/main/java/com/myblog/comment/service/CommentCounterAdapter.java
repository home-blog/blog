package com.myblog.comment.service;

import com.myblog.comment.repository.CommentRepository;
import com.myblog.comment.repository.CommentRepository.PostCount;
import com.myblog.post.PostCommentCounter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 글 모듈의 PostCommentCounter를 채운다 → 글 상세의 commentCount (specs/005 T020), 글 관리의 댓글 수 (specs/006 T019). */
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

    /** 한 쪽의 글 번호를 GROUP BY 쿼리 하나로 센다. 댓글이 없는 글은 0. */
    @Override
    public Map<Long, Long> countByPostIds(Collection<Long> postIds) {
        Map<Long, Long> counts = new HashMap<>();
        postIds.forEach(id -> counts.put(id, 0L));
        if (!postIds.isEmpty()) {
            for (PostCount row : comments.countByPostIds(postIds)) {
                counts.put(row.getPostId(), row.getCommentCount());
            }
        }
        return counts;
    }
}
