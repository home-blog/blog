package com.myblog.community;

import java.time.Instant;
import java.util.List;

/**
 * 블로그 글 신고를 편집실에 알려 주는 입구 (specs/018 D-1, D-3). 편집실 모듈이 부르고 좋아요·신고 모듈이 자기 표에 쓴다.
 * 검토 대기 = 아직 닫지 않은 신고(resolved_at 없음). 신고한 사람은 알려 주지 않는다.
 */
public interface PostReports {

    /** 대기 신고가 있는 글 수. */
    long pendingPostCount();

    /** 대기 신고 수 (운영 현황). */
    long pendingReportCount();

    /** 대기 신고가 있는 글, 신고가 많은 글 먼저, 같으면 첫 신고가 오래된 글 먼저. */
    List<PendingPost> pendingPosts(long offset, int limit);

    /** 한 글의 대기 신고, 오래된 순. */
    List<ReportLine> pendingOf(Long postId);

    /** 한 글의 대기 신고를 모두 닫는다. resolution은 taken_down(내림)·dismissed(반려). 닫은 수를 돌려준다. */
    int resolve(Long postId, String resolution, Instant now);

    record PendingPost(Long postId, long reportCount, long spam, long abuse, long adult, long other, Instant firstAt,
            Instant lastAt) {
    }

    /** reason은 SPAM·ABUSE·ADULT·OTHER, detail은 OTHER일 때만 있을 수 있다. */
    record ReportLine(String reason, String detail, Instant createdAt) {
    }
}
