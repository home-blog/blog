package com.myblog.blog;

import java.time.Instant;
import java.util.Optional;

/**
 * "블로그 주인이 댓글 관리를 마지막으로 연 시각" (blog.comments_read_at, 내 확장 E-6, specs/006 T032, D-8, research B-6, R-4).
 * 새 댓글 수는 이 시각보다 늦게 남이 단 댓글이다. 댓글 모듈이 이것으로 묻고 바꾼다 (블로그 표를 직접 건드리지 않는다).
 * <p>
 * 읽음 처리와 댓글 쓰기가 겹쳐도 댓글이 "보지도 못하고 사라지지" 않게(R-4) 둘 다 블로그 줄 잠금으로 줄을 선다:
 * 읽음 처리는 쓰기 잠금, 댓글 쓰기는 {@link #holdForComment}로 함께 읽기 잠금을 잡은 뒤 작성 시각을 정한다.
 */
public interface CommentReadMarks {

    /** 마지막으로 연 시각. 한 번도 열지 않았으면 비어 있다. */
    Optional<Instant> readAt(Long blogId);

    /** 지금(서버 시각)을 마지막으로 연 시각으로 바꾸고 이전 값을 돌려준다. 시각은 뒤로 가지 않는다. */
    ReadMark markRead(Long blogId);

    /**
     * 이 블로그 글에 댓글을 쓰기 전에 부른다 (같은 트랜잭션 안에서). 진행 중인 읽음 처리가 끝날 때까지 기다린다.
     * 그래서 댓글의 작성 시각은 읽음 처리 시각보다 늦거나, 읽음 처리가 그 댓글을 이미 본 뒤다.
     */
    void holdForComment(Long blogId);

    /** previous: 이전에 연 시각 (처음이면 null), readAt: 이번에 연 시각. */
    record ReadMark(Instant previous, Instant readAt) {
    }
}
