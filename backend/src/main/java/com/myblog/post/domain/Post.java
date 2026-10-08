package com.myblog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 글 (post 표). 글에는 블로그와 작성자 칸이 없다: 블로그는 분류를 따라가서 알고, 작성자는 그 블로그의 주인이다 (003 data-model 3).
 * 본문은 마크다운 원문을 그대로 둔다. 서버는 HTML로 바꾸지 않는다 (003 D-1).
 * <p>
 * 작성 시각은 저장할 때 자동으로 채운다. 수정 시각은 @LastModifiedDate를 쓰지 않는다:
 * 바뀐 것이 있을 때만 서비스가 직접 넣는다 (research R-2, FR-020).
 */
@Entity
@Table(name = "post")
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "visibility", nullable = false, length = 10)
    private String visibility;

    @Column(name = "views", nullable = false)
    private int views;

    @Column(name = "request_key", length = 36, unique = true, updatable = false)
    private String requestKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Post() {
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getVisibility() {
        return visibility;
    }

    public int getViews() {
        return views;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
