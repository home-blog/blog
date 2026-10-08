package com.myblog.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 댓글 (comment 표, specs/005 data-model 1). 고치는 방법은 없다 (FR-005).
 * 대댓글(parent_id)과 비밀 댓글(is_secret)은 칸만 있고 쓰지 않는다: 늘 비어 있고 거짓이다 (D-7).
 * 작성 시각은 Clock으로 넣는다: 5초 간격을 같은 시계로 센다 (D-2).
 */
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "parent_id", updatable = false)
    private Long parentId;

    @Column(name = "body", nullable = false, length = 500, updatable = false)
    private String body;

    @Column(name = "is_secret", nullable = false, updatable = false)
    private boolean secret;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
    }

    /** 새 댓글. 내용은 normalizeBody로 정리해 저장한다. */
    public static Comment write(Long postId, Long memberId, String body, Instant now) {
        Comment comment = new Comment();
        comment.postId = postId;
        comment.memberId = memberId;
        comment.body = normalizeBody(body);
        comment.createdAt = now;
        return comment;
    }

    /** 줄바꿈(\r\n, \r)을 \n으로 맞추고 앞뒤 공백·줄바꿈을 지운다 (FR-002, research B-2). 없으면 빈 글자. */
    public static String normalizeBody(String body) {
        return body == null ? "" : body.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getBody() {
        return body;
    }

    public boolean isSecret() {
        return secret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
