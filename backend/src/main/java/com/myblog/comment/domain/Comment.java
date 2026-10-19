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
 * 답글(parent_id)은 한 단계: 늘 원댓글을 가리킨다. 비밀 댓글(is_secret)은 쓴 사람·블로그 주인·원댓글 쓴 사람만 읽는다 (specs/019).
 * 답글이 남은 원댓글을 지우면 줄은 남고 내용이 빈 "지운 칸"(deleted_at)이 된다. 지운 칸은 저장소의 update 문으로만 만든다.
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

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private Instant deletedAt;

    protected Comment() {
    }

    /** 새 댓글. 내용은 normalizeBody로 정리해 저장한다. */
    public static Comment write(Long postId, Long memberId, String body, Instant now) {
        return write(postId, memberId, body, null, false, now);
    }

    /** 새 댓글이나 답글. parentId는 원댓글 번호(원댓글이면 null). */
    public static Comment write(Long postId, Long memberId, String body, Long parentId, boolean secret, Instant now) {
        Comment comment = new Comment();
        comment.postId = postId;
        comment.memberId = memberId;
        comment.body = normalizeBody(body);
        comment.parentId = parentId;
        comment.secret = secret;
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

    public boolean isReply() {
        return parentId != null;
    }

    /** 지운 칸 (답글이 남아 줄만 남은 원댓글). */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 이 댓글을 읽을 수 있나. 비밀이 아니면 누구나, 비밀이면 쓴 사람·블로그 주인·(답글이면) 원댓글 쓴 사람.
     *
     * @param parentWriterId 원댓글 쓴 사람 (원댓글이면 null)
     */
    public boolean readableBy(Long viewerId, boolean viewerOwnsPost, Long parentWriterId) {
        if (!secret) {
            return true;
        }
        return viewerId != null && (viewerOwnsPost || viewerId.equals(memberId) || viewerId.equals(parentWriterId));
    }
}
