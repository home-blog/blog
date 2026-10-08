package com.myblog.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 글 신고 (post_report 표, specs/005 data-model 4). 접수만 저장한다: 처리 상태는 없고 글을 숨기지도 않는다 (FR-020).
 * 줄은 PostReportRepository.insertIfAbsent로 넣는다 (한 회원이 한 글에 한 번, E-7). 엔티티는 읽기와 표 확인(validate)용이다.
 */
@Entity
@Table(name = "post_report")
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_report_id")
    private Long id;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "reason", nullable = false, length = 20, updatable = false)
    private String reason;

    @Column(name = "detail", length = 200, updatable = false)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostReport() {
    }

    /**
     * 저장할 설명: 기타(OTHER)일 때만, 줄바꿈을 맞추고 앞뒤 공백을 지운 값. 비면 저장하지 않는다(null).
     * 다른 사유에 보낸 설명은 버린다 (research B-10).
     */
    public static String detailToStore(ReportReason reason, String detail) {
        if (reason != ReportReason.OTHER) {
            return null;
        }
        String normalized = normalizeDetail(detail);
        return normalized.isEmpty() ? null : normalized;
    }

    /** 줄바꿈(\r\n, \r)을 \n으로 맞추고 앞뒤 공백을 지운다. 없으면 빈 글자. */
    public static String normalizeDetail(String detail) {
        return detail == null ? "" : detail.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPostId() {
        return postId;
    }

    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
