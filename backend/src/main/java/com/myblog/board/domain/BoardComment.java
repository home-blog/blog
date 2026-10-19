package com.myblog.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 글마당 댓글 (board_comment 표, specs/017 data-model). 고치는 방법은 없다(블로그 댓글과 같음). */
@Entity
@Table(name = "board_comment")
public class BoardComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_comment_id")
    private Long id;

    @Column(name = "board_post_id", nullable = false, updatable = false)
    private Long boardPostId;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long writerId;

    @Column(name = "content", nullable = false, length = 1000, updatable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BoardComment() {
    }

    public static BoardComment write(Long boardPostId, Long writerId, String content, Instant now) {
        BoardComment comment = new BoardComment();
        comment.boardPostId = boardPostId;
        comment.writerId = writerId;
        comment.content = content;
        comment.createdAt = now;
        return comment;
    }

    public boolean isWrittenBy(Long memberId) {
        return memberId != null && memberId.equals(writerId);
    }

    public Long getId() {
        return id;
    }

    public Long getBoardPostId() {
        return boardPostId;
    }

    public Long getWriterId() {
        return writerId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
