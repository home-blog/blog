package com.myblog.board.repository;

import com.myblog.board.domain.BoardComment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글마당 댓글 (specs/017 US3). */
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    /** 오래된 순 (S3-1). 채택한 댓글을 맨 위로 올리는 일은 서비스가 한다. */
    List<BoardComment> findByBoardPostIdOrderByCreatedAtAscIdAsc(Long boardPostId);

    @Query("select max(c.createdAt) from BoardComment c where c.writerId = :writerId")
    Optional<Instant> findLastCreatedAt(@Param("writerId") Long writerId);
}
