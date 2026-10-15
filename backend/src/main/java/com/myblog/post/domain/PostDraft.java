package com.myblog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 임시 저장·예약 글 (post_draft 표, specs/013 data-model 1). publishAt이 비어 있으면 임시 저장, 있으면 예약.
 * 글 표와 따로 두어 공개 글 쿼리에 나오지 않는다 (D-1). 발행할 때 requestKey를 글의 request_key로 써서 두 번 생기지 않게 한다 (D-3).
 * 입력 검사는 DraftService가 마친 뒤에 채운다. 태그는 줄바꿈으로 이어 한 칸에 둔다(발행할 때 글 태그 규칙으로 검사).
 */
@Entity
@Table(name = "post_draft")
public class PostDraft {

    private static final String TAG_SEPARATOR = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_draft_id")
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "series_id")
    private Long seriesId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "visibility", nullable = false, length = 10)
    private String visibility;

    @Column(name = "tags", nullable = false, length = 200)
    private String tags;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "request_key", nullable = false, length = 36, unique = true, updatable = false)
    private String requestKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PostDraft() {
    }

    public static PostDraft start(Long blogId, Instant now) {
        PostDraft draft = new PostDraft();
        draft.blogId = blogId;
        draft.requestKey = UUID.randomUUID().toString();
        draft.createdAt = now;
        return draft;
    }

    /** 칸을 모두 바꾼다. 예약 시각은 그대로 둔다 (예약된 글을 다시 저장해도 예약은 남는다). */
    public void fill(Long categoryId, Long topicId, Long seriesId, String title, String content, String visibility,
            List<String> tags, Instant now) {
        this.categoryId = categoryId;
        this.topicId = topicId;
        this.seriesId = seriesId;
        this.title = Post.normalizeTitle(title);
        this.content = Post.normalizeContent(content);
        this.visibility = visibility;
        this.tags = joinTags(tags);
        this.updatedAt = now;
    }

    public void schedule(Instant publishAt) {
        this.publishAt = publishAt;
    }

    public void unschedule() {
        this.publishAt = null;
    }

    public static String joinTags(List<String> tags) {
        return tags == null ? "" : String.join(TAG_SEPARATOR, tags.stream().map(tag -> tag == null ? "" : tag).toList());
    }

    public List<String> tagList() {
        return tags == null || tags.isEmpty() ? List.of() : Arrays.asList(tags.split(TAG_SEPARATOR, -1));
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public Long getSeriesId() {
        return seriesId;
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

    public Instant getPublishAt() {
        return publishAt;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
