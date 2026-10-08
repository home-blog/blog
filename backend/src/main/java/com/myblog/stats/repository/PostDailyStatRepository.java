package com.myblog.stats.repository;

import com.myblog.stats.domain.PostDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글 일별 조회수 (specs/006 T004, E-5). 값은 파라미터로만 넘긴다. */
public interface PostDailyStatRepository extends JpaRepository<PostDailyStat, Long> {

    /** 글의 일별 조회수를 모두 지운다 (글을 지울 때, T010). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostDailyStat s where s.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);
}
