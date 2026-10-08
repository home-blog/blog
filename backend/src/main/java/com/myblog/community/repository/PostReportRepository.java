package com.myblog.community.repository;

import com.myblog.community.domain.PostReport;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글 신고 (specs/005 T037). 값은 파라미터로만 넘긴다. */
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    /**
     * 이 회원이 이 글을 아직 신고하지 않았을 때만 넣는다 (E-7의 UNIQUE). 넣었으면 1, 이미 있었으면 0.
     * 거의 동시에 두 번 와도 하나만 1이다 (research R-1). 오류로 트랜잭션이 깨지지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into post_report (users_id, post_id, reason, detail, created_at)"
            + " values (:memberId, :postId, :reason, :detail, :now) on conflict (users_id, post_id) do nothing",
            nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId, @Param("postId") Long postId, @Param("reason") String reason,
            @Param("detail") String detail, @Param("now") Instant now);

    /** 글이 지워질 때 그 글의 신고도 지운다 (D-11, 003 D-7). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostReport r where r.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);
}
