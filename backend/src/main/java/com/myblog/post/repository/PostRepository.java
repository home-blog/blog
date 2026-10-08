package com.myblog.post.repository;

import com.myblog.post.domain.Post;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByRequestKey(String requestKey);

    /** 이 분류들 중 가장 늦게 쓴 글 (같은 시각이면 번호가 큰 글). 글쓰기 기본 분류·주제에 쓴다 (research B-6). */
    Optional<Post> findFirstByCategoryIdInOrderByCreatedAtDescIdDesc(Collection<Long> categoryIds);

    /** 분류의 글 개수 (비공개 글 포함). */
    long countByCategoryId(Long categoryId);

    /** 분류마다 공개 글 개수. 글이 없는 분류는 결과에 없다. */
    @Query("select p.categoryId as categoryId, count(p) as postCount from Post p"
            + " where p.categoryId in :categoryIds and p.visibility = 'public' group by p.categoryId")
    List<CategoryCount> countPublicByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

    interface CategoryCount {

        Long getCategoryId();

        long getPostCount();
    }
}
