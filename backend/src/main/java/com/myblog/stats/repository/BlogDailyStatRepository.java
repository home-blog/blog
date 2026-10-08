package com.myblog.stats.repository;

import com.myblog.stats.domain.BlogDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 블로그 일별 통계 (specs/006 T004). 값은 파라미터로만 넘긴다. */
public interface BlogDailyStatRepository extends JpaRepository<BlogDailyStat, Long> {

    /** 블로그의 일별 통계를 모두 지운다 (블로그를 닫을 때, T010). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlogDailyStat s where s.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}
