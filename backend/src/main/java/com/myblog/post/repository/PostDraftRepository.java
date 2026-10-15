package com.myblog.post.repository;

import com.myblog.post.domain.PostDraft;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostDraftRepository extends JpaRepository<PostDraft, Long> {

    List<PostDraft> findByBlogIdOrderByUpdatedAtDescIdDesc(Long blogId);

    long countByBlogId(Long blogId);

    Optional<PostDraft> findByIdAndBlogId(Long id, Long blogId);

    /**
     * 시각이 지난 예약 하나를 잠그고 번호를 준다 (D-3). 다른 벌이 잠근 줄은 건너뛰어(SKIP LOCKED) 두 벌이 같은 예약을 올리지 않는다.
     * 잠금은 부른 트랜잭션이 끝날 때 풀린다.
     */
    @Query(value = """
            select post_draft_id from post_draft
            where publish_at is not null and publish_at <= :now
            order by publish_at, post_draft_id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<Long> lockNextDue(@Param("now") Instant now);

    /** 분류의 예약된 글 수 (예약된 글이 쓰는 분류는 지우지 못하게, S4-6). */
    long countByCategoryIdAndPublishAtIsNotNull(Long categoryId);


    /** 지운 줄 수. 두 요청이 같은 임시 저장 글을 함께 발행해도 둘째는 0이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostDraft d where d.id = :id")
    int deleteDraft(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PostDraft d set d.publishAt = null where d.id = :id")
    int unschedule(@Param("id") Long id);

    @Modifying
    @Query("delete from PostDraft d where d.blogId = :blogId")
    void deleteByBlogId(@Param("blogId") Long blogId);
}
