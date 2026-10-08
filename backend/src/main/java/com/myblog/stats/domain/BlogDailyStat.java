package com.myblog.stats.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 블로그 일별 통계 (blog_daily_stat 표, specs/006 data-model 6). stat_date는 한국 날짜다.
 * 줄은 그날 첫 조회·방문 때 생긴다. 숫자는 "없으면 만들고 있으면 +1"을 한 쿼리로 올린다 (BlogDailyStatRepository, R-3).
 * 이 객체로는 읽기만 한다.
 */
@Entity
@Table(name = "blog_daily_stat")
public class BlogDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_stat_id")
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "stat_date", nullable = false, updatable = false)
    private LocalDate statDate;

    @Column(name = "views", nullable = false)
    private int views;

    @Column(name = "visitors", nullable = false)
    private int visitors;

    protected BlogDailyStat() {
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public int getViews() {
        return views;
    }

    public int getVisitors() {
        return visitors;
    }
}
