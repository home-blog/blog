package com.myblog.subscription.repository;

import com.myblog.subscription.domain.BlogSubscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 원고 받아 보기 (specs/016 US1). 값은 파라미터로만 넘긴다. */
public interface BlogSubscriptionRepository extends JpaRepository<BlogSubscription, Long> {

    /** 없으면 넣고, 있으면 메일 받기만 바꾼다. 동시에 두 번 와도 UNIQUE(users_id, blog_id)가 한 줄로 지킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into blog_subscription (users_id, blog_id, email_enabled, created_at)"
            + " values (:memberId, :blogId, :emailEnabled, :now)"
            + " on conflict (users_id, blog_id) do update set email_enabled = excluded.email_enabled", nativeQuery = true)
    int upsert(@Param("memberId") Long memberId, @Param("blogId") Long blogId,
            @Param("emailEnabled") boolean emailEnabled, @Param("now") Instant now);

    Optional<BlogSubscription> findByMemberIdAndBlogId(Long memberId, Long blogId);

    boolean existsByMemberIdAndBlogId(Long memberId, Long blogId);

    long countByBlogId(Long blogId);

    long countByMemberId(Long memberId);

    List<BlogSubscription> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    @Query("select s.blogId from BlogSubscription s where s.memberId = :memberId")
    List<Long> findBlogIdsByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlogSubscription s where s.memberId = :memberId and s.blogId = :blogId")
    int deleteByMemberIdAndBlogId(@Param("memberId") Long memberId, @Param("blogId") Long blogId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BlogSubscription s set s.emailEnabled = false where s.id = :id")
    int disableEmail(@Param("id") Long id);

    /** 탈퇴할 때: 그 사람이 받아 보던 것. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlogSubscription s where s.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);

    /** 블로그를 닫을 때: 그 블로그를 받아 보던 줄. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlogSubscription s where s.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}
