package com.myblog.community.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.community.controller.dto.ReportRequest;
import com.myblog.community.domain.PostReport;
import com.myblog.community.domain.ReportReason;
import com.myblog.community.repository.PostReportRepository;
import com.myblog.post.PostLookup;
import com.myblog.post.PostLookup.PostRef;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글 신고 (specs/005 contracts 6, FR-017 ~ FR-021).
 * 검사 순서 (research B-1): 볼 수 있는 글인가(POST_NOT_FOUND) → 자기 글(SELF_REPORT_NOT_ALLOWED) → 입력 → 이미 신고(ALREADY_REPORTED) → 저장.
 * 접수만 저장한다. 글을 숨기거나 지우는 일은 하지 않는다 (FR-020).
 */
@Service
public class ReportService {

    private final PostReportRepository reports;
    private final PostLookup postLookup;
    private final Validator validator;
    private final Clock clock;

    public ReportService(PostReportRepository reports, PostLookup postLookup, Validator validator, Clock clock) {
        this.reports = reports;
        this.postLookup = postLookup;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public void report(Long memberId, Long postId, ReportRequest request) {
        PostRef post = postLookup.findVisible(postId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (post.isOwnedBy(memberId)) {
            throw new ApiException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
        Set<ConstraintViolation<ReportRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        ReportReason reason = ReportReason.parse(request.reason()).orElseThrow();
        int inserted = reports.insertIfAbsent(memberId, post.postId(), reason.name(),
                PostReport.detailToStore(reason, request.detail()), Instant.now(clock));
        if (inserted == 0) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }
    }
}
