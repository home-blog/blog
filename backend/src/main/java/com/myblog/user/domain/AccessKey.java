package com.myblog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 개인 열쇠 (access_key 표, specs/020 data-model). 열쇠 원문은 만들 때 한 번만 돌려주고 저장하지 않는다:
 * SHA-256 해시와 화면에 보일 앞 8글자만 둔다. 마지막 사용 시각은 저장소의 update 문으로만 고친다.
 */
@Entity
@Table(name = "access_key")
public class AccessKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_key_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "name", nullable = false, length = 40, updatable = false)
    private String name;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String tokenHash;

    @Column(name = "token_hint", nullable = false, length = 12, updatable = false)
    private String tokenHint;

    @Column(name = "scope", nullable = false, length = 10, updatable = false)
    private String scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at", insertable = false, updatable = false)
    private Instant lastUsedAt;

    protected AccessKey() {
    }

    public static AccessKey issue(Long memberId, String name, String tokenHash, String tokenHint, String scope,
            Instant now, Instant expiresAt) {
        AccessKey key = new AccessKey();
        key.memberId = memberId;
        key.name = name;
        key.tokenHash = tokenHash;
        key.tokenHint = tokenHint;
        key.scope = scope;
        key.createdAt = now;
        key.expiresAt = expiresAt;
        return key;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getTokenHint() {
        return tokenHint;
    }

    public String getScope() {
        return scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
