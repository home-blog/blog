package com.myblog.blog.repository;

import com.myblog.blog.domain.Blog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findByOwnerId(Long ownerId);
}
