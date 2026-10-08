package com.myblog.blog.repository;

import com.myblog.blog.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByBlogIdOrderBySortOrderAsc(Long blogId);

    /** 주인이 정한 순서. 순서 값이 같으면 먼저 만든 분류가 위 (003 data-model 2). */
    List<Category> findByBlogIdOrderBySortOrderAscIdAsc(Long blogId);

    /** 블로그 안에서 소문자로 맞춘 이름이 같은 분류 (자기 자신은 빼고, 새 분류면 excludeId에 null). */
    @Query("select count(c) > 0 from Category c where c.blogId = :blogId and lower(c.name) = lower(:name)"
            + " and (:excludeId is null or c.id <> :excludeId)")
    boolean existsSameName(@Param("blogId") Long blogId, @Param("name") String name, @Param("excludeId") Long excludeId);

    @Query("select coalesce(max(c.sortOrder), 0) from Category c where c.blogId = :blogId")
    int maxSortOrder(@Param("blogId") Long blogId);

    /** 블로그의 분류를 모두 지운다 (미분류 포함, 탈퇴 때만. specs/002 T032). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Category c where c.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}
