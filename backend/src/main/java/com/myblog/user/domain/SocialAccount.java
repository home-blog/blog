package com.myblog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 회원과 소셜 계정의 연결 (user_social_account 표, specs/010 T001, E-8).
 * provider는 SocialProvider.id()(github, google, kakao, naver). 회사 토큰은 두지 않는다 (D-7).
 */
@Entity
@Table(name = "user_social_account")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_social_account_id")
    private Long id;

    @Column(name = "users_id", nullable = false)
    private Long userId;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected SocialAccount() {
    }

    private SocialAccount(Long userId, String provider, String providerUserId, Instant linkedAt) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.linkedAt = linkedAt;
    }

    public static SocialAccount link(Long userId, String provider, String providerUserId, Instant now) {
        return new SocialAccount(userId, provider, providerUserId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
