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

    /** 답글 소식(specs/019 D-4): 원댓글 쓴 사람 한 사람에게. 탈퇴했으면 넣지 않는다. */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
            + " select :memberId, 'reply', :postId, :title, :actorName, :now"
            + " where exists (select 1 from users u where u.users_id = :memberId and u.deleted_at is null)",
            nativeQuery = true)
    int insertReplyNotice(@Param("memberId") Long memberId, @Param("postId") Long postId,
            @Param("title") String title, @Param("actorName") String actorName, @Param("now") Instant now);

    /** 글마당 소식(specs/017): kind는 board(내 글에 댓글) · accepted(내 댓글이 채택됨). 받는 사람 한 사람에게. */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, board_post_id, post_title, actor_name, created_at)"
            + " select :memberId, :kind, :boardPostId, :title, :actorName, :now"
            + " where exists (select 1 from users u where u.users_id = :memberId and u.deleted_at is null)",
            nativeQuery = true)
    int insertBoardNotice(@Param("memberId") Long memberId, @Param("kind") String kind,
            @Param("boardPostId") Long boardPostId, @Param("title") String title, @Param("actorName") String actorName,
            @Param("now") Instant now);

    /** 편집실 소식(specs/018): 글 주인 한 사람에게, 사유(note)와 함께. 지운 글마당 글이면 postId가 비어 있다. */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, post_id, post_title, actor_name, note, created_at)"
            + " select :memberId, 'editorial', :postId, :title, '편집실', :note, :now"
            + " where exists (select 1 from users u where u.users_id = :memberId and u.deleted_at is null)",
            nativeQuery = true)
    int insertEditorialNotice(@Param("memberId") Long memberId, @Param("postId") Long postId,
            @Param("title") String title, @Param("note") String note, @Param("now") Instant now);

    /**
     * 방명록 소식(specs/021): kind는 guestbook(내 방명록에 새 엽서) · gb_reply(내 엽서에 답장). 받는 사람 한 사람에게,
     * 블로그 이름은 post_title 칸에. 그사이 엽서가 지워졌거나 받는 사람이 탈퇴했으면 넣지 않는다.
     */
    @Modifying
    @Query(value = "insert into notification (users_id, kind, blog_id, guestbook_entry_id, post_title, actor_name,"
            + " created_at)"
            + " select :memberId, :kind, e.blog_id, e.guestbook_entry_id, :blogName, :actorName, :now"
            + " from guestbook_entry e where e.guestbook_entry_id = :entryId"
            + " and exists (select 1 from users u where u.users_id = :memberId and u.deleted_at is null)"
            // 엽서마다 같은 소식은 한 번만: 답장을 지웠다 다시 써도 우편함을 채우지 않는다
            + " and not exists (select 1 from notification n where n.users_id = :memberId"
            + " and n.guestbook_entry_id = :entryId and n.kind = :kind)",
            nativeQuery = true)
    int insertGuestbookNotice(@Param("memberId") Long memberId, @Param("kind") String kind,
            @Param("entryId") Long entryId, @Param("blogName") String blogName, @Param("actorName") String actorName,
            @Param("now") Instant now);

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
