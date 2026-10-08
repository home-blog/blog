package com.myblog.comment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;

/**
 * 통계 화면의 일별 댓글 수를 묻는 입구 (specs/006 T009, FR-032, FR-036). 통계 모듈이 묻고 댓글 모듈이 채운다.
 * 댓글 수는 따로 저장하지 않고 댓글 표에서 센다 (지운 댓글은 지난 날짜에서도 빠진다, research R-5).
 */
public interface CommentStatsQuery {

    /**
     * 글 번호들의 댓글을 작성 시각을 zone의 날짜로 바꿔 묶은 수. 기간은 [from, to) 순간으로 비교한다.
     * 댓글이 없는 날은 결과에 없다.
     */
    Map<LocalDate, Long> dailyCounts(Collection<Long> postIds, Instant from, Instant to, ZoneId zone);
}
