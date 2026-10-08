package com.myblog.image.repository;

import com.myblog.image.domain.PostImage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 글 이미지 기록 (specs/005 T052 ~ T058). 값은 파라미터로만 넘긴다. */
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    Optional<PostImage> findByStorageKey(String storageKey);

    long countByPostId(Long postId);

    /** 이 회원이 올렸는데 아직 글에 연결되지 않은 이미지 수 (새 글의 10장 세기, D-3). */
    @Query("select count(i) from PostImage i where i.memberId = :memberId and i.postId is null")
    long countUnlinked(@Param("memberId") Long memberId);

    /** 본문에 든 이미지 중 이 글에 연결할 수 있는 것: 이미 이 글의 것, 또는 이 회원이 올린 연결 전 이미지. */
    @Query("select i from PostImage i where i.storageKey in :keys"
            + " and (i.postId = :postId or (i.postId is null and i.memberId = :memberId))")
    List<PostImage> findLinkable(@Param("keys") Collection<String> keys, @Param("postId") Long postId,
            @Param("memberId") Long memberId);

    List<PostImage> findByPostId(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PostImage i set i.postId = :postId where i.id in :ids and i.postId is null")
    int link(@Param("ids") Collection<Long> ids, @Param("postId") Long postId);

    /**
     * 정리 시간보다 오래된 연결 전 이미지의 기록을 지우고, 실제로 지운 것의 파일 이름을 돌려준다 (D-3).
     * 고르기와 지우기가 한 문장이라, 그사이 글 저장이 연결한 이미지(post_id가 채워진 것)는 지우지 않는다.
     */
    @Query(value = "delete from post_image where post_id is null and created_at < :before returning storage_key",
            nativeQuery = true)
    List<String> deleteUnlinkedBefore(@Param("before") Instant before);

    /** 받은 이름 중 기록이 있는 것 (저장소에만 남은 파일 찾기, D-5). */
    @Query("select i.storageKey from PostImage i where i.storageKey in :keys")
    List<String> findExistingKeys(@Param("keys") Collection<String> keys);
}
