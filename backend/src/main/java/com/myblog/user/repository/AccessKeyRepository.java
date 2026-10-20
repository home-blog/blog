package com.myblog.user.repository;

import com.myblog.user.domain.AccessKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 개인 열쇠 (specs/020). 값은 파라미터로만 넘긴다. */
public interface AccessKeyRepository extends JpaRepository<AccessKey, Long> {

    List<AccessKey> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    long countByMemberId(Long memberId);

    Optional<AccessKey> findByTokenHash(String tokenHash);

    Optional<AccessKey> findByIdAndMemberId(Long id, Long memberId);

    /** 마지막 사용 시각: 처음이거나 since보다 오래됐을 때만 고친다(요청마다 쓰지 않게). */
    @Modifying
    @Query(value = "update access_key set last_used_at = :now where access_key_id = :id"
            + " and (last_used_at is null or last_used_at < :since)", nativeQuery = true)
    int touch(@Param("id") Long id, @Param("now") Instant now, @Param("since") Instant since);

    @Modifying
    @Query("delete from AccessKey k where k.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}
