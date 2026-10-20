package com.myblog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 회원 (users 표, specs/001 data-model.md 1).
 * 이메일은 앞뒤 공백을 지우고 소문자로 맞춰 저장한다. 탈퇴한 회원(deletedAt 있음)은 로그인·중복 검사 대상이 아니다.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** BCrypt 해시. 원문은 어디에도 남기지 않는다 (FR-010). 소셜로만 가입한 회원은 비어 있다 (specs/010 D-4). */
    @Column(name = "password", length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "intro", length = 100)
    private String intro;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** 역할: member(기본) 또는 admin. 관리자는 운영자가 DB에서 정한다 (specs/012 D-1, D-3). */
    @Column(name = "role", nullable = false, length = 10)
    private String role = ROLE_MEMBER;

    /** 이 시각 전까지 이용 정지 (specs/022 D-4). 지나면 저절로 풀린다. */
    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(name = "suspend_reason", length = 200)
    private String suspendReason;

    static final String ROLE_MEMBER = "member";
    static final String ROLE_ADMIN = "admin";

    /** 탈퇴한 회원의 값 틀 (data-model 3의 12번, D-2). 이메일 .invalid는 메일이 갈 수 없는 이름이다. */
    static final String WITHDRAWN_EMAIL = "deleted-%d@deleted.invalid";
    /** BCrypt 모양이 아니라 어떤 비밀번호와도 맞지 않는다. */
    static final String WITHDRAWN_PASSWORD = "!deleted";
    static final String WITHDRAWN_NICKNAME = "탈퇴한사용자%d";

    protected User() {
    }

    private User(String email, String passwordHash, String nickname) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.nickname = nickname.strip();
        this.failedLoginCount = 0;
    }

    /**
     * 새 회원. passwordHash는 이미 BCrypt로 바꾼 값이어야 하고, 소셜 가입이면 null이다.
     * 가입 시각은 저장할 때 자동으로 채워진다.
     */
    public static User create(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname);
    }

    /** 이메일을 저장·비교하는 모양으로 맞춘다: 앞뒤 공백 제거, 소문자. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * 닉네임과 소개를 고친다 (002 FR-005, FR-006). 이메일을 바꾸는 메서드는 두지 않는다.
     * 닉네임은 앞뒤 공백을 지우고, 소개는 입력 그대로 두되 비어 있으면 NULL로 저장한다 (002 research B-4).
     */
    public void changeProfile(String nickname, String intro) {
        this.nickname = nickname.strip();
        this.intro = intro == null || intro.isEmpty() ? null : intro;
    }

    /** 비밀번호를 바꾼다. 원문은 받지 않고 BCrypt로 바꾼 값만 받는다 (CF-01-8, 002 FR-019). */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 탈퇴 (002 FR-030, D-1, D-2). 회원 줄은 지우지 않고(남는 댓글이 가리킨다) 탈퇴 시각을 남기며,
     * 이메일·비밀번호·닉네임·소개를 알아볼 수 없는 값으로 바꾼다. 그래서 같은 이메일·닉네임으로 다시 가입할 수 있다.
     */
    public void withdraw(Instant now) {
        this.deletedAt = now;
        this.email = WITHDRAWN_EMAIL.formatted(id);
        this.passwordHash = WITHDRAWN_PASSWORD;
        this.nickname = WITHDRAWN_NICKNAME.formatted(id);
        this.intro = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 잠금 시각이 아직 지나지 않았으면 잠긴 상태다 (FR-027 ~ 029). */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** 정지 중인가 (specs/022 S2-5): 정지 시각이 아직 오지 않았다. 바로 그 시각부터는 정지가 아니다. */
    public boolean isSuspended(Instant now) {
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }

    /** 정지한다 (specs/022 US2). 다시 정지하면 기간과 사유를 새로 정한다 (S2-4). */
    public void suspend(Instant until, String reason) {
        this.suspendedUntil = until;
        this.suspendReason = reason;
    }

    /** 정지를 푼다. 정지 중이 아니었으면(없거나 기한이 지남) 거짓. 기한이 지난 칸도 함께 비운다. */
    public boolean unsuspend(Instant now) {
        boolean was = isSuspended(now);
        this.suspendedUntil = null;
        this.suspendReason = null;
        return was;
    }

    /** 관리자인지 (specs/012). 요청마다 DB에서 읽은 값으로 본다 (D-2). */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
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

    public String getRole() {
        return role;
    }

    public Instant getSuspendedUntil() {
        return suspendedUntil;
    }

    public String getSuspendReason() {
        return suspendReason;
    }
}
