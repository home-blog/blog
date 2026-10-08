package com.myblog.stats.repository;

import com.myblog.stats.domain.PostDailyStat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글 일별 조회수 (specs/006 T004, E-5). 값은 파라미터로만 넘긴다. */
public interface PostDailyStatRepository extends JpaRepository<PostDailyStat, Long> {

    /** 글마다 from 날짜부터의 조회수 합 (인기 글, D-6). 조회가 없던 글은 결과에 없다. */
    @Query("select s.postId as postId, sum(s.views) as views from PostDailyStat s"
            + " where s.postId in :postIds and s.statDate >= :from group by s.postId")
    List<PostViews> sumViewsSince(@Param("postIds") Collection<Long> postIds, @Param("from") LocalDate from);

    /** 그날 글 조회수 +1. 줄이 없으면 만든다 (한 쿼리, research R-3). */
    @Modifying
    @Query(value = "insert into post_daily_stat (post_id, stat_date, views) values (:postId, :date, 1)"
            + " on conflict (post_id, stat_date) do update set views = post_daily_stat.views + 1", nativeQuery = true)
    int addView(@Param("postId") Long postId, @Param("date") LocalDate date);

    interface PostViews {

        Long getPostId();

        long getViews();
    }

    /** 글의 일별 조회수를 모두 지운다 (글을 지울 때, T010). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostDailyStat s where s.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);
}
