package com.myblog.community.repository;

import com.myblog.community.domain.PostLike;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 좋아요 (specs/005 T030). 값은 파라미터로만 넘긴다. */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 아직 없을 때만 넣는다. 거의 동시에 두 번 와도 DB의 UNIQUE(users_id, post_id)가 하나만 받고 나머지는 조용히 넘어간다
     * (오류로 트랜잭션이 깨지지 않는다, research R-1, B-8). 넣었으면 1, 이미 있었으면 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into post_like (post_id, users_id, created_at) values (:postId, :memberId, :now)"
            + " on conflict (users_id, post_id) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("postId") Long postId, @Param("memberId") Long memberId, @Param("now") Instant now);

    long countByPostId(Long postId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostLike l where l.postId = :postId and l.memberId = :memberId")
    int deleteByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);

    /** 글이 지워질 때 (T032). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostLike l where l.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    /** 회원이 탈퇴할 때: 그 회원이 누른 좋아요 모두 (T032, 002 D-5). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostLike l where l.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}
