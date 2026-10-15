package com.myblog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 연재 (series 표, specs/013 data-model 2). 입력 검사는 SeriesService가 마친 뒤에 만든다. 연재에 든 글은 series_post 표. */
@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "series_id")
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Series() {
    }

    public static Series create(Long blogId, String name, String description, Instant now) {
        Series series = new Series();
        series.blogId = blogId;
        series.name = name;
        series.description = description;
        series.createdAt = now;
        return series;
    }

    public void change(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
