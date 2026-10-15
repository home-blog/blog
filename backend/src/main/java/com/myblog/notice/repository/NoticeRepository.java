package com.myblog.notice.repository;

import com.myblog.notice.domain.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 공지사항 표. 목록은 고정 먼저, 그 안에서 새 글 먼저(specs/012 D-7). 같은 시각이면 번호가 큰 글이 먼저. */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("select n from Notice n order by n.pinned desc, n.createdAt desc, n.id desc")
    List<Notice> findPage(Pageable pageable);

    @Query("select n from Notice n where n.kind = :kind order by n.pinned desc, n.createdAt desc, n.id desc")
    List<Notice> findPageByKind(String kind, Pageable pageable);

    long countByKind(String kind);

    Optional<Notice> findFirstByOrderByCreatedAtDesc();
}
