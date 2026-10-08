package com.myblog.blog.repository;

import com.myblog.blog.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByBlogIdOrderBySortOrderAsc(Long blogId);

    /** 주인이 정한 순서. 순서 값이 같으면 먼저 만든 분류가 위 (003 data-model 2). */
    List<Category> findByBlogIdOrderBySortOrderAscIdAsc(Long blogId);
}
