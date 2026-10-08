package com.myblog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * 회원 (users 표, specs/001 data-model.md 1).
 * 이메일은 앞뒤 공백을 지우고 소문자로 맞춰 저장한다. 탈퇴한 회원(deletedAt 있음)은 로그인·중복 검사 대상이 아니다.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** BCrypt 해시. 원문은 어디에도 남기지 않는다 (FR-010). */
    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "intro", length = 100)
    private String intro;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    private User(String email, String passwordHash, String nickname, Instant createdAt) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.nickname = nickname.strip();
        this.createdAt = createdAt;
        this.failedLoginCount = 0;
    }

    /** 새 회원. passwordHash는 이미 BCrypt로 바꾼 값이어야 한다. */
    public static User create(String email, String passwordHash, String nickname, Instant now) {
        return new User(email, passwordHash, nickname, now);
    }

    /** 이메일을 저장·비교하는 모양으로 맞춘다: 앞뒤 공백 제거, 소문자. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 잠금 시각이 아직 지나지 않았으면 잠긴 상태다 (FR-027 ~ 029). */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getIntro() {
        return intro;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
