package com.myblog.stats.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 글 일별 조회수 (post_daily_stat 표, 내 확장 E-5, specs/006 data-model 4, D-6). 인기 글(최근 7일 조회수 순)에 쓴다.
 * 이 객체로는 읽기만 한다. 숫자는 PostDailyStatRepository의 한 쿼리로 올린다.
 */
@Entity
@Table(name = "post_daily_stat")
public class PostDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_daily_stat_id")
    private Long id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "stat_date", nullable = false, updatable = false)
    private LocalDate statDate;

    @Column(name = "views", nullable = false)
    private int views;

    protected PostDailyStat() {
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public int getViews() {
        return views;
    }
}
