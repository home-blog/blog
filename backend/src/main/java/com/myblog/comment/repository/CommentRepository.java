package com.myblog.comment.repository;

import com.myblog.comment.domain.Comment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 댓글 조회 (specs/005 T016). 값은 파라미터로만 넘긴다. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 글의 댓글, 오래된 순 (같은 시각이면 먼저 만든 것이 위, FR-003). */
    @Query("select c from Comment c where c.postId = :postId order by c.createdAt, c.id")
    List<Comment> findByPostIdOldestFirst(@Param("postId") Long postId);

    long countByPostId(Long postId);

    /** 글마다 댓글 수 (specs/006 T019). 댓글이 없는 글은 결과에 없다. */
    @Query("select c.postId as postId, count(c) as commentCount from Comment c where c.postId in :postIds group by c.postId")
    List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /** 회원의 마지막 댓글 시각 (5초 간격, D-2). 어느 글에 썼는지는 상관없다. */
    @Query("select max(c.createdAt) from Comment c where c.memberId = :memberId")
    Optional<Instant> findLastCreatedAt(@Param("memberId") Long memberId);

    /** 글의 댓글을 모두 지운다 (글 삭제, T025). 대댓글은 쓰지 않으므로(D-7) 한 번에 지워도 서로 가리키는 줄이 없다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    interface PostCount {

        Long getPostId();

        long getCommentCount();
    }
}
