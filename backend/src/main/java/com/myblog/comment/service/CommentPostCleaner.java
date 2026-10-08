package com.myblog.comment.service;

import com.myblog.comment.repository.CommentRepository;
import com.myblog.post.PostDeletingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 글이 지워지면 그 글의 댓글을 먼저 지운다 (specs/005 T025, FR-006, data-model 7).
 * 글 모듈이 글을 지우기 전에 낸 PostDeletingEvent를 <b>같은 트랜잭션 안에서</b> 듣는다: 하나라도 실패하면 글 삭제 전체가 취소된다.
 * 탈퇴하면 그 회원의 블로그 글마다 이 이벤트가 오므로(003 PostBlogClosingCleaner) BlogClosingEvent는 따로 듣지 않는다.
 * MemberWithdrawnEvent도 듣지 않는다: 탈퇴한 회원이 남의 글에 단 댓글은 남고 "탈퇴한 사용자"로 보인다 (FR-007, 002 D-1).
 */
@Component
public class CommentPostCleaner {

    private final CommentRepository comments;

    public CommentPostCleaner(CommentRepository comments) {
        this.comments = comments;
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        comments.deleteByPostId(event.postId());
    }
}
