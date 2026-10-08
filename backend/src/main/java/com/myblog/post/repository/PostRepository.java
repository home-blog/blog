package com.myblog.post.repository;

import com.myblog.post.domain.Post;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByRequestKey(String requestKey);

    List<Post> findByCategoryIdIn(Collection<Long> categoryIds);

    /** 이 분류들 중 가장 늦게 쓴 글 (같은 시각이면 번호가 큰 글). 글쓰기 기본 분류·주제에 쓴다 (research B-6). */
    Optional<Post> findFirstByCategoryIdInOrderByCreatedAtDescIdDesc(Collection<Long> categoryIds);

    /** (작성 시각, 글 번호)가 기준보다 앞인 공개 글 중 가장 가까운 것. 첫 줄만 쓴다 (이전 글, research B-5). */
    @Query("select p.id from Post p where p.categoryId in :categoryIds and p.visibility = 'public'"
            + " and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :postId))"
            + " order by p.createdAt desc, p.id desc")
    List<Long> findPreviousIds(@Param("categoryIds") Collection<Long> categoryIds, @Param("createdAt") Instant createdAt,
            @Param("postId") Long postId, Pageable pageable);

    /** (작성 시각, 글 번호)가 기준보다 뒤인 공개 글 중 가장 가까운 것 (다음 글). */
    @Query("select p.id from Post p where p.categoryId in :categoryIds and p.visibility = 'public'"
            + " and (p.createdAt > :createdAt or (p.createdAt = :createdAt and p.id > :postId))"
            + " order by p.createdAt asc, p.id asc")
    List<Long> findNextIds(@Param("categoryIds") Collection<Long> categoryIds, @Param("createdAt") Instant createdAt,
            @Param("postId") Long postId, Pageable pageable);

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

    /*
     * 004 글 목록 (T009): 분류 묶음 안의 글. 어느 분류·어떤 글을 읽을지는 PostVisibility.listScope가 정한다.
     * 정렬과 건너뛸 수는 Pageable로 값으로 넘긴다 (작성 시각 내림차순, 같으면 큰 번호가 위).
     */

    long countByCategoryIdIn(Collection<Long> categoryIds);

    long countByCategoryIdInAndVisibility(Collection<Long> categoryIds, String visibility);

    List<Post> findByCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    List<Post> findByCategoryIdInAndVisibility(Collection<Long> categoryIds, String visibility, Pageable pageable);

    interface CategoryCount {

        Long getCategoryId();

        long getPostCount();
    }
}
