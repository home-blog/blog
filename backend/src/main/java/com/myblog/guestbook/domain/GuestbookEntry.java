package com.myblog.guestbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 방명록 엽서 (guestbook_entry 표, specs/021 data-model). 엽서 내용은 고치지 않는다(범위 밖). 답장은 블로그 주인만 하나 쓰고 고친다.
 * 비밀 엽서는 쓴 사람과 블로그 주인만 읽는다.
 */
@Entity
@Table(name = "guestbook_entry")
public class GuestbookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guestbook_entry_id")
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long writerId;

    @Column(name = "body", nullable = false, length = 300, updatable = false)
    private String body;

    @Column(name = "stamp", nullable = false, length = 10, updatable = false)
    private String stamp;

    @Column(name = "is_secret", nullable = false, updatable = false)
    private boolean secret;

    @Column(name = "reply", length = 300)
    private String reply;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GuestbookEntry() {
    }

    public static GuestbookEntry write(Long blogId, Long writerId, String body, Stamp stamp, boolean secret,
            Instant now) {
        GuestbookEntry entry = new GuestbookEntry();
        entry.blogId = blogId;
        entry.writerId = writerId;
        entry.body = body;
        entry.stamp = stamp.code();
        entry.secret = secret;
        entry.createdAt = now;
        return entry;
    }

    /** 줄바꿈(\r\n, \r)을 \n으로 맞추고 앞뒤 공백·줄바꿈을 지운다. 없으면 빈 글자. */
    public static String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    /**
     * 답장을 쓰거나 고친다.
     * @return 처음 쓴 답장인가 (그때만 소식을 보낸다, S2-2)
     */
    public boolean reply(String text, Instant now) {
        boolean first = repliedAt == null;
        reply = text;
        if (first) {
            repliedAt = now;
        }
        return first;
    }

    public void clearReply() {
        reply = null;
        repliedAt = null;
    }

    /** 이 엽서를 읽을 수 있나: 비밀이 아니면 누구나, 비밀이면 쓴 사람과 블로그 주인. */
    public boolean readableBy(Long viewerId, Long ownerId) {
        return !secret || viewerId != null && (viewerId.equals(writerId) || viewerId.equals(ownerId));
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public Long getWriterId() {
        return writerId;
    }

    public String getBody() {
        return body;
    }

    public String getStamp() {
        return stamp;
    }

    public boolean isSecret() {
        return secret;
    }

    public String getReply() {
        return reply;
    }

    public Instant getRepliedAt() {
        return repliedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
