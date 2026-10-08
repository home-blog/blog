package com.myblog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 주제 (topic 표). 서비스 전체의 분류(여행, 음식, 취미, 운동, 개발)로, 글마다 하나를 고른다 (003 FR-046). 읽기만 한다. */
@Entity
@Table(name = "topic")
public class Topic {

    @Id
    @Column(name = "topic_id")
    private Long id;

    @Column(name = "topic_name", nullable = false, length = 10)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Topic() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
