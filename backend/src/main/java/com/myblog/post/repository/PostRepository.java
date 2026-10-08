package com.myblog.post.repository;

import com.myblog.post.domain.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 분류의 글 개수 (비공개 글 포함). */
    long countByCategoryId(Long categoryId);

    /** 분류마다 모든 글 개수 (비공개 포함). 글이 없는 분류는 결과에 없다. */
    @Query("select p.categoryId as categoryId, count(p) as postCount from Post p"
            + " where p.categoryId in :categoryIds group by p.categoryId")
    List<CategoryCount> countAllByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

    /** 분류마다 공개 글 개수. 글이 없는 분류는 결과에 없다. */
    @Query("select p.categoryId as categoryId, count(p) as postCount from Post p"
            + " where p.categoryId in :categoryIds and p.visibility = 'public' group by p.categoryId")
    List<CategoryCount> countPublicByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

    interface CategoryCount {

        Long getCategoryId();

        long getPostCount();
    }
}
