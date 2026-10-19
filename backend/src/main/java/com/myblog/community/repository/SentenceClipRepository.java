package com.myblog.community.repository;

import com.myblog.community.domain.SentenceClip;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 모은 문장 (specs/015 US3). 값은 파라미터로만 넘긴다. */
public interface SentenceClipRepository extends JpaRepository<SentenceClip, Long> {

    /** 아직 없을 때만 넣는다. UNIQUE(users_id, post_id, text)에 걸리면 조용히 넘어간다. 넣었으면 1, 이미 있었으면 0. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into sentence_clip (users_id, post_id, post_title, blog_name, text, created_at)"
            + " values (:memberId, :postId, :postTitle, :blogName, :text, :now)"
            + " on conflict (users_id, post_id, text) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId, @Param("postId") Long postId,
            @Param("postTitle") String postTitle, @Param("blogName") String blogName, @Param("text") String text,
            @Param("now") Instant now);

    Optional<SentenceClip> findByMemberIdAndPostIdAndText(Long memberId, Long postId, String text);

    long countByMemberId(Long memberId);

    /** 필사 노트: 최근 순 (같은 시각이면 나중에 넣은 것 먼저). */
    Page<SentenceClip> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SentenceClip c where c.id = :clipId and c.memberId = :memberId")
    int deleteByIdAndMemberId(@Param("clipId") Long clipId, @Param("memberId") Long memberId);

    /** 이 글에서 문장을 모은 사람 수 (한 사람이 여러 문장을 모아도 1명). */
    @Query("select count(distinct c.memberId) from SentenceClip c where c.postId = :postId")
    long countCollectors(@Param("postId") Long postId);

    /** 탈퇴할 때: 그 회원의 필사 노트 모두. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SentenceClip c where c.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);

    /** 글이 지워질 때: 문장은 남기고 원문 자리만 비운다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SentenceClip c set c.postId = null where c.postId = :postId")
    int clearPost(@Param("postId") Long postId);
}
