package com.myblog.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 글 좋아요 (post_like 표, specs/005 data-model 2). 한 회원이 한 글에 하나 (DB의 UNIQUE).
 * 줄은 PostLikeRepository.insertIfAbsent로 넣는다 (동시에 눌러도 오류 없이 하나만). 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "post_like")
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostLike() {
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
