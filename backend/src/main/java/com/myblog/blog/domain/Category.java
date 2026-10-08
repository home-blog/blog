package com.myblog.blog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 분류 (category 표). 한 블로그 안에서 주인이 만들고, 비공개로 할 수 있다. 칸과 규칙은 specs/003이 정한다. */
@Entity
@Table(name = "category")
@EntityListeners(AuditingEntityListener.class)
public class Category {

    /** 가입할 때 함께 만드는 기본 분류 (003 FR-003). */
    public static final String DEFAULT_NAME = "미분류";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "blog_id", nullable = false)
    private Long blogId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "intro", length = 255)
    private String intro;

    @Column(name = "visibility", nullable = false, length = 10)
    private String visibility;

    @Column(name = "is_default", nullable = false)
    private boolean defaultCategory;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "color_index", nullable = false)
    private short colorIndex;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Category() {
    }

    /** 기본 분류 "미분류": 공개, 맨 앞, 기본 분류 표시. */
    public static Category defaultFor(Long blogId) {
        Category category = new Category();
        category.blogId = blogId;
        category.name = DEFAULT_NAME;
        category.visibility = "public";
        category.defaultCategory = true;
        category.sortOrder = 1; // data-model 2: 기본 분류가 맨 앞
        category.colorIndex = 0;
        return category;
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public String getName() {
        return name;
    }

    public String getIntro() {
        return intro;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isDefaultCategory() {
        return defaultCategory;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public short getColorIndex() {
        return colorIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
