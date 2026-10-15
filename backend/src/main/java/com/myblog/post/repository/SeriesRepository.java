package com.myblog.post.repository;

import com.myblog.post.domain.Series;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    long countByBlogId(Long blogId);

    List<Series> findByBlogIdOrderByCreatedAtAscIdAsc(Long blogId);

    @Modifying
    @Query("delete from Series s where s.blogId = :blogId")
    void deleteByBlogId(Long blogId);
}
