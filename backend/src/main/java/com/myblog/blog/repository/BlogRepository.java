package com.myblog.blog.repository;

import com.myblog.blog.domain.Blog;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findByOwnerId(Long ownerId);

    /** 블로그 줄을 쓰기 잠금으로 읽는다 (댓글 읽음 처리, specs/006 T032). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Blog b where b.id = :blogId")
    Optional<Blog> findForUpdateById(@Param("blogId") Long blogId);

    /** 블로그 줄에 읽기 잠금을 건다 (댓글 쓰기가 읽음 처리와 줄을 서게, specs/006 R-4). */
    @Query(value = "select blog_id from blog where blog_id = :blogId for share", nativeQuery = true)
    List<Long> lockForShareById(@Param("blogId") Long blogId);
}
