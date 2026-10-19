package com.myblog.board.repository;

import com.myblog.board.domain.BoardPost;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글마당 글 (specs/017). 조회 수·댓글 수는 읽고 쓰지 않고 DB에서 바로 더한다(겹쳐도 빠지지 않게, D-3·D-4). */
public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    /** 같은 회원의 마지막 글 시각 (너무 빨리 올리기, D-6). */
    @Query("select max(p.createdAt) from BoardPost p where p.writerId = :writerId")
    Optional<Instant> findLastCreatedAt(@Param("writerId") Long writerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update board_post set views = views + 1 where board_post_id = :id", nativeQuery = true)
    int incrementViews(@Param("id") Long id);

    /** 댓글 수를 delta만큼. 0보다 작아지지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update board_post set comment_count = greatest(comment_count + :delta, 0) where board_post_id = :id",
            nativeQuery = true)
    int adjustCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /** 채택 칸을 비운다. 글을 지우기 전에 불러, 댓글이 함께 지워질 때(ON DELETE CASCADE) 지워지는 글을 다시 고치지 않게 한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update board_post set accepted_comment_id = null where board_post_id = :id", nativeQuery = true)
    int clearAccepted(@Param("id") Long id);

    /** 글을 지운다. 댓글은 ON DELETE CASCADE로 함께 지워진다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from board_post where board_post_id = :id", nativeQuery = true)
    int deletePost(@Param("id") Long id);

    @Query("select p.id from BoardPost p where p.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
}
