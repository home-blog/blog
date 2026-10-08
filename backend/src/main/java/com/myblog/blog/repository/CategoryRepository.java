package com.myblog.blog.repository;

import com.myblog.blog.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByBlogIdOrderBySortOrderAsc(Long blogId);
}
