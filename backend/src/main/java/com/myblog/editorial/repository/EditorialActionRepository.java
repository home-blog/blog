package com.myblog.editorial.repository;

import com.myblog.editorial.domain.EditorialAction;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 편집실 처리 기록 (specs/018 D-4). */
public interface EditorialActionRepository extends JpaRepository<EditorialAction, Long> {

    List<EditorialAction> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
