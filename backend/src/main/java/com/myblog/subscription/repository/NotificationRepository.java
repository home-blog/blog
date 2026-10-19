package com.myblog.subscription.repository;

import com.myblog.subscription.domain.Notification;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 우편함 소식 (specs/016 US2). 값은 파라미터로만 넘긴다. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 새 글 소식: 이 블로그를 받아 보는 사람(탈퇴하지 않은)마다 한 줄. 같은 글로 두 번 넣지 않는다(부분 UNIQUE). 넣은 수를 돌려준다.
     */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
            + " select s.users_id, 'post', :postId, :title, :blogName, :now from blog_subscription s"
            + " join users u on u.users_id = s.users_id and u.deleted_at is null"
            + " where s.blog_id = :blogId and s.users_id <> :ownerId"
            + " on conflict (users_id, post_id) where kind = 'post' do nothing", nativeQuery = true)
    int insertPostNotices(@Param("blogId") Long blogId, @Param("ownerId") Long ownerId, @Param("postId") Long postId,
            @Param("title") String title, @Param("blogName") String blogName, @Param("now") Instant now);

    /** 댓글 소식: 글 주인 한 사람에게. */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
            + " values (:memberId, 'comment', :postId, :title, :actorName, :now)", nativeQuery = true)
    int insertCommentNotice(@Param("memberId") Long memberId, @Param("postId") Long postId,
            @Param("title") String title, @Param("actorName") String actorName, @Param("now") Instant now);

    Page<Notification> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

    long countByMemberId(Long memberId);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    boolean existsByIdAndMemberId(Long id, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.id = :id and n.memberId = :memberId and n.readAt is null")
    int markRead(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.memberId = :memberId and n.readAt is null")
    int markAllRead(@Param("memberId") Long memberId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);

    /** 보관 기간이 지난 소식. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
