package com.myblog.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 우편함 소식 (notification 표, specs/016 data-model). kind는 post(받아 보는 블로그의 새 글) · comment(내 글에 달린 댓글)
 * · board(내 글마당 글에 달린 댓글) · accepted(내 글마당 댓글이 채택됨, specs/017) · editorial(편집실, specs/018)
 * · reply(내 댓글에 답글, specs/019). 블로그 글 소식은 postId, 글마당 소식은 boardPostId.
 * 글이 지워지면 번호가 비고, 그때의 제목·보낸 이름은 남는다. 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "kind", nullable = false, length = 10, updatable = false)
    private String kind;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "board_post_id")
    private Long boardPostId;

    @Column(name = "post_title", nullable = false, length = 100, updatable = false)
    private String postTitle;

    @Column(name = "actor_name", nullable = false, length = 30, updatable = false)
    private String actorName;

    /** 편집실 소식의 사유 (specs/018). 다른 소식은 비어 있다. */
    @Column(name = "note", length = 200, updatable = false)
    private String note;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getKind() {
        return kind;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getBoardPostId() {
        return boardPostId;
    }

    public String getNote() {
        return note;
    }

    public boolean isEditorial() {
        return "editorial".equals(kind);
    }

    public boolean isBoard() {
        return "board".equals(kind) || "accepted".equals(kind);
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getActorName() {
        return actorName;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
