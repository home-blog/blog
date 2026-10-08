package com.myblog.blog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 블로그 (blog 표). 회원 한 명에 블로그 하나. 칸과 규칙은 specs/003이 정한다. */
@Entity
@Table(name = "blog")
public class Blog {

    /** 가입할 때 만드는 블로그 이름: "{닉네임}의 블로그" (003 FR-002). */
    public static final String DEFAULT_NAME_FORMAT = "%s의 블로그";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private Long id;

    @Column(name = "users_id", nullable = false, unique = true)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "intro", length = 500)
    private String intro;

    @Column(name = "comments_read_at")
    private Instant commentsReadAt;

    protected Blog() {
    }

    private Blog(Long ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
    }

    /** 가입과 함께 만드는 블로그. 소개는 비워 둔다. */
    public static Blog createFor(Long ownerId, String nickname) {
        return new Blog(ownerId, DEFAULT_NAME_FORMAT.formatted(nickname));
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getIntro() {
        return intro;
    }

    public Instant getCommentsReadAt() {
        return commentsReadAt;
    }
}
