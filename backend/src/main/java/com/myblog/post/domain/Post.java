package com.myblog.post.domain;

import com.myblog.common.Visibility;
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

    /**
     * 새 글 (FR-010 ~ FR-014). 제목은 앞뒤 공백을 지우고, 본문은 줄바꿈만 \n으로 맞추고 그 밖에는 손대지 않는다 (D-2).
     * 공개 여부가 없으면 공개. 작성 시각은 저장할 때 채워지고 수정 시각은 비어 있다.
     */
    public static Post create(Long categoryId, Long topicId, String title, String content, String visibility,
            String requestKey) {
        Post post = new Post();
        post.categoryId = categoryId;
        post.topicId = topicId;
        post.title = normalizeTitle(title);
        post.content = normalizeContent(content);
        post.visibility = visibility == null ? Visibility.PUBLIC.value() : visibility;
        post.requestKey = requestKey;
        return post;
    }

    /**
     * 글 수정 (FR-020, research B-7, R-2). 정리한 값을 지금 값과 하나씩 비교해 <b>하나라도 다를 때만</b> 바꾸고
     * 수정 시각을 now로 넣는다. 바뀌었는지를 돌려준다. 작성 시각은 바꾸는 방법이 없다.
     */
    public boolean update(Long categoryId, Long topicId, String title, String content, String visibility, Instant now) {
        if (sameAs(categoryId, topicId, title, content, visibility)) {
            return false;
        }
        this.categoryId = categoryId;
        this.topicId = topicId;
        this.title = normalizeTitle(title);
        this.content = normalizeContent(content);
        this.visibility = visibility == null ? Visibility.PUBLIC.value() : visibility;
        this.updatedAt = now;
        return true;
    }

    /** 글 칸은 그대로인데 딸린 것(태그)만 바뀌었을 때 수정 시각을 넣는다 (005 T044, 2026-10-08 가안: 태그만 바꿔도 "수정됨"). */
    public void markUpdated(Instant now) {
        this.updatedAt = now;
    }

    /** 정리한 값이 지금 값과 모두 같은가 (같은 요청 번호로 다시 온 글쓰기가 처음과 같은지 볼 때). */
    public boolean sameAs(Long categoryId, Long topicId, String title, String content, String visibility) {
        String newVisibility = visibility == null ? Visibility.PUBLIC.value() : visibility;
        return this.categoryId.equals(categoryId) && this.topicId.equals(topicId) && this.title.equals(normalizeTitle(title))
                && this.content.equals(normalizeContent(content)) && this.visibility.equals(newVisibility);
    }

    /** 제목은 앞뒤 공백을 지운다. 없으면 빈 글자. */
    public static String normalizeTitle(String title) {
        return title == null ? "" : title.strip();
    }

    /** 본문은 줄바꿈(\r\n)만 \n으로 맞춘다. 앞뒤 공백은 지우지 않는다. 없으면 빈 글자. */
    public static String normalizeContent(String content) {
        return content == null ? "" : content.replace("\r\n", "\n");
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
