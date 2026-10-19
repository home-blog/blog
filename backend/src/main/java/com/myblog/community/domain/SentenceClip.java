package com.myblog.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 모은 문장 (sentence_clip 표, specs/015 US3). 원문 글이 지워지면 postId가 비고, 모을 때의 제목·블로그 이름은 남는다.
 * 줄은 SentenceClipRepository.insertIfAbsent로 넣는다 (같은 문장을 다시 모아도 오류 없이 하나만). 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "sentence_clip")
public class SentenceClip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_clip_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "post_title", nullable = false, length = 100, updatable = false)
    private String postTitle;

    @Column(name = "blog_name", nullable = false, length = 30, updatable = false)
    private String blogName;

    @Column(name = "text", nullable = false, length = 300, updatable = false)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SentenceClip() {
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

    public String getPostTitle() {
        return postTitle;
    }

    public String getBlogName() {
        return blogName;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
