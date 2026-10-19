package com.myblog.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 원고 받아 보기 (blog_subscription 표, specs/016 data-model). 한 사람이 한 블로그에 하나 (DB의 UNIQUE).
 * 줄은 BlogSubscriptionRepository.upsert로 넣는다. 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "blog_subscription")
public class BlogSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_subscription_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BlogSubscription() {
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getBlogId() {
        return blogId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
