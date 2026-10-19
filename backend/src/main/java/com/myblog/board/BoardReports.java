package com.myblog.board;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 글마당 글 신고를 편집실에 알려 주고 처리하는 입구 (specs/018 US2, D-1, D-5). 편집실 모듈이 부르고 글마당 모듈이 자기 표에 쓴다.
 * 검토 대기 = 아직 닫지 않은 신고. 신고한 사람은 알려 주지 않는다.
 */
public interface BoardReports {

    long pendingPostCount();

    long pendingReportCount();

    /** 대기 신고가 있는 글, 신고가 많은 글 먼저, 같으면 첫 신고가 오래된 글 먼저. */
    List<PendingBoardPost> pendingPosts(long offset, int limit);

    /** 한 글의 대기 신고, 오래된 순. */
    List<BoardReportLine> pendingOf(Long boardPostId);

    /** 글 줄을 잠근다(편집실 처리끼리 차례로). 없는 글이면 거짓. */
    boolean lock(Long boardPostId);

    /** 글 하나(쓴 사람, 제목, 본문 앞부분). 없으면 비운다. */
    Optional<BoardBrief> brief(Long boardPostId);

    /** 대기 신고를 모두 반려로 닫는다. 닫은 수를 돌려준다. */
    int dismiss(Long boardPostId, Instant now);

    /** 글을 지운다(댓글·신고도 함께). 지운 글 수(0 또는 1)를 돌려준다. */
    int remove(Long boardPostId);

    record PendingBoardPost(Long boardPostId, long reportCount, long spam, long abuse, long adult, long other,
            Instant firstAt, Instant lastAt) {
    }

    record BoardReportLine(String reason, String detail, Instant createdAt) {
    }

    record BoardBrief(Long boardPostId, Long writerId, String topic, String title, String excerpt) {
    }
}
