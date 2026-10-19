package com.myblog.comment.repository;

import com.myblog.comment.domain.Comment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 댓글 조회 (specs/005 T016). 값은 파라미터로만 넘긴다. 수 세기·블로그 관리 목록은 지운 칸(specs/019)을 뺀다. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 글의 댓글, 오래된 순 (같은 시각이면 먼저 만든 것이 위, FR-003). */
    @Query("select c from Comment c where c.postId = :postId order by c.createdAt, c.id")
    List<Comment> findByPostIdOldestFirst(@Param("postId") Long postId);

    /** 글의 댓글 수 (지운 칸 빼고). */
    @Query("select count(c) from Comment c where c.postId = :postId and c.deletedAt is null")
    long countByPostId(@Param("postId") Long postId);

    /** 글마다 댓글 수 (specs/006 T019, 지운 칸 빼고). 댓글이 없는 글은 결과에 없다. */
    @Query("select c.postId as postId, count(c) as commentCount from Comment c"
            + " where c.postId in :postIds and c.deletedAt is null group by c.postId")
    List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /** 회원의 마지막 댓글 시각 (5초 간격, D-2). 어느 글에 썼는지는 상관없다. */
    @Query("select max(c.createdAt) from Comment c where c.memberId = :memberId")
    Optional<Instant> findLastCreatedAt(@Param("memberId") Long memberId);

    /** 글의 댓글을 모두 지운다 (글 삭제, T025). 답글과 원댓글이 같은 문장에서 지워지므로 외래 키가 막지 않는다 (specs/019). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    /* 006 블로그 관리: 내 블로그 글들의 댓글 (ManageCommentQueryService, NewCommentCounterService) */

    /** 글 번호들의 댓글, 최신순 (같은 시각이면 번호가 큰 것이 위). 쪽은 Pageable로. */
    @Query("select c from Comment c where c.postId in :postIds and c.deletedAt is null"
            + " order by c.createdAt desc, c.id desc")
    List<Comment> findByPostIdsNewestFirst(@Param("postIds") Collection<Long> postIds, Pageable pageable);

    @Query("select count(c) from Comment c where c.postId in :postIds and c.deletedAt is null")
    long countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    /** 새 댓글: 주인이 쓰지 않았고 since보다 늦은 것. */
    @Query("select count(c) from Comment c where c.postId in :postIds and c.memberId <> :ownerId and c.createdAt > :since"
            + " and c.deletedAt is null")
    long countNewSince(@Param("postIds") Collection<Long> postIds, @Param("ownerId") Long ownerId,
            @Param("since") Instant since);

    /** 한 번도 열지 않았을 때의 새 댓글: 주인이 쓰지 않은 모든 댓글. */
    @Query("select count(c) from Comment c where c.postId in :postIds and c.memberId <> :ownerId and c.deletedAt is null")
    long countNotBy(@Param("postIds") Collection<Long> postIds, @Param("ownerId") Long ownerId);

    /* 019 답글: 원댓글 줄을 잠가 "답글 수를 센 뒤 지운다"와 "부모를 확인한 뒤 답글을 넣는다"를 한 줄로 세운다 (research D-2) */

    /**
     * 답글 쓰기: 부모 줄을 FOR SHARE로 잡는다 (지우기의 FOR UPDATE와 엇갈린다). 지워졌거나 지운 칸이면 비어 있다.
     * 잠금을 기다린 뒤 PostgreSQL은 WHERE를 새 줄로 다시 보므로, 기다리는 사이 지운 칸이 되어도 여기서 걸린다.
     */
    @Query(value = "select comment_id from comment where comment_id = :id and deleted_at is null for share",
            nativeQuery = true)
    Optional<Long> lockShared(@Param("id") Long id);

    /** 지우기: 원댓글 줄을 FOR UPDATE로 잡는다. */
    @Query(value = "select comment_id from comment where comment_id = :id for update", nativeQuery = true)
    Optional<Long> lockForUpdate(@Param("id") Long id);

    /** 원댓글 번호 (원댓글이면 자기 번호). 엔티티를 읽지 않고 번호만. */
    @Query("select coalesce(c.parentId, c.id) from Comment c where c.id = :id")
    Optional<Long> findRootIdOf(@Param("id") Long id);

    long countByParentId(Long parentId);

    /** 지운 칸으로 만든다: 내용을 비우고 지운 시각을 적는다. 쓴 사람 번호는 남긴다 (비밀 답글을 원댓글 쓴 사람이 읽게). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update comment set body = '', deleted_at = :now where comment_id = :id", nativeQuery = true)
    int markDeleted(@Param("id") Long id, @Param("now") Instant now);

    interface PostCount {

        Long getPostId();

        long getCommentCount();
    }
}
