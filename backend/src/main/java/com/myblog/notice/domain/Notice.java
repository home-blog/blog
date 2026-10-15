package com.myblog.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 공지사항 (notice 표, specs/012 data-model 2). 입력 검사는 NoticeService가 마친 뒤에 만든다. */
@Entity
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "release_version", length = 20)
    private String releaseVersion;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long writerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Notice() {
    }

    public static Notice write(NoticeKind kind, String title, String content, String releaseVersion, boolean pinned,
            Long writerId, Instant now) {
        Notice notice = new Notice();
        notice.fill(kind, title, content, releaseVersion, pinned);
        notice.writerId = writerId;
        notice.createdAt = now;
        return notice;
    }

    public void change(NoticeKind kind, String title, String content, String releaseVersion, boolean pinned, Instant now) {
        fill(kind, title, content, releaseVersion, pinned);
        this.updatedAt = now;
    }

    private void fill(NoticeKind kind, String title, String content, String releaseVersion, boolean pinned) {
        this.kind = kind.value();
        this.title = title;
        this.content = content;
        this.releaseVersion = releaseVersion;
        this.pinned = pinned;
    }

    public Long getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public boolean isPinned() {
        return pinned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
