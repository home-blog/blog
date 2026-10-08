package com.myblog.stats.repository;

import com.myblog.stats.domain.BlogDailyStat;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 블로그 일별 통계 (specs/006 T004). 값은 파라미터로만 넘긴다. */
public interface BlogDailyStatRepository extends JpaRepository<BlogDailyStat, Long> {

    /** 블로그의 [from, to] 날짜 줄. 기록이 없는 날은 줄이 없다. */
    List<BlogDailyStat> findByBlogIdAndStatDateBetween(Long blogId, LocalDate from, LocalDate to);

    /** 누적 = 모든 줄의 합 (D-7). 줄이 없으면 0. */
    @Query("select coalesce(sum(s.views), 0) as views, coalesce(sum(s.visitors), 0) as visitors"
            + " from BlogDailyStat s where s.blogId = :blogId")
    Totals totalsOf(@Param("blogId") Long blogId);

    /** 그날 조회수 +1. 줄이 없으면 만든다 (한 쿼리, research R-3). */
    @Modifying
    @Query(value = "insert into blog_daily_stat (blog_id, stat_date, views, visitors) values (:blogId, :date, 1, 0)"
            + " on conflict (blog_id, stat_date) do update set views = blog_daily_stat.views + 1", nativeQuery = true)
    int addView(@Param("blogId") Long blogId, @Param("date") LocalDate date);

    /** 그날 방문자 +1. 줄이 없으면 만든다. */
    @Modifying
    @Query(value = "insert into blog_daily_stat (blog_id, stat_date, views, visitors) values (:blogId, :date, 0, 1)"
            + " on conflict (blog_id, stat_date) do update set visitors = blog_daily_stat.visitors + 1", nativeQuery = true)
    int addVisitor(@Param("blogId") Long blogId, @Param("date") LocalDate date);

    interface Totals {

        long getViews();

        long getVisitors();
    }

    /** 블로그의 일별 통계를 모두 지운다 (블로그를 닫을 때, T010). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlogDailyStat s where s.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}
