package com.myblog.guestbook.repository;

import com.myblog.guestbook.domain.GuestbookEntry;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 방명록 엽서 (specs/021). 값은 파라미터로만 넘긴다. */
public interface GuestbookEntryRepository extends JpaRepository<GuestbookEntry, Long> {

    /** 한 블로그의 엽서, 최근 순 (같은 시각이면 나중 번호가 위). */
    Page<GuestbookEntry> findByBlogIdOrderByCreatedAtDescIdDesc(Long blogId, Pageable pageable);

    long countByBlogId(Long blogId);

    /** 이 회원의 마지막 엽서 시각 (어느 블로그든, S1-4 간격). */
    @Query("select max(e.createdAt) from GuestbookEntry e where e.writerId = :writerId")
    Optional<Instant> findLastCreatedAt(@Param("writerId") Long writerId);

    /** 이 회원이 이 블로그에 since 뒤로 남긴 엽서 수 (S1-4 하루 한도). */
    long countByWriterIdAndBlogIdAndCreatedAtGreaterThanEqual(Long writerId, Long blogId, Instant since);

    /** 답장·지우기는 엽서 줄을 잠그고 한다 (같은 엽서에 두 번 답해도 소식은 한 번). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from GuestbookEntry e where e.id = :id")
    Optional<GuestbookEntry> findForUpdate(@Param("id") Long id);
}
