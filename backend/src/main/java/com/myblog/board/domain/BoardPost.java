package com.myblog.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 글마당 글 (board_post 표, specs/017 data-model). 조회 수·댓글 수는 DB에서 바로 더하고 빼므로(D-3, D-4) 엔티티로는 쓰지 않는다.
 * 채택한 댓글(acceptedCommentId)은 물음에만 있다.
 */
@Entity
@Table(name = "board_post")
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_post_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long writerId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "views", nullable = false, insertable = false, updatable = false)
    private long views;

    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private int commentCount;

    @Column(name = "accepted_comment_id")
    private Long acceptedCommentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BoardPost() {
    }

    public static BoardPost write(Long writerId, Long topicId, BoardKind kind, String title, String content, Instant now) {
        BoardPost post = new BoardPost();
        post.writerId = writerId;
        post.topicId = topicId;
        post.kind = kind.value();
        post.title = title;
        post.content = content;
        post.createdAt = now;
        post.updatedAt = now;
        return post;
    }

    public void change(Long topicId, BoardKind kind, String title, String content, Instant now) {
        this.topicId = topicId;
        this.kind = kind.value();
        this.title = title;
        this.content = content;
        this.updatedAt = now;
    }

    public void accept(Long commentId) {
        this.acceptedCommentId = commentId;
    }

    public boolean isWrittenBy(Long memberId) {
        return memberId != null && memberId.equals(writerId);
    }

    public boolean isQuestion() {
        return BoardKind.QUESTION.value().equals(kind);
    }

    public Long getId() {
        return id;
    }

    public Long getWriterId() {
        return writerId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getViews() {
        return views;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public Long getAcceptedCommentId() {
        return acceptedCommentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
