package com.myblog.post;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 블로그 관리(댓글 관리, 대시보드)가 글에 대해 묻는 입구 (specs/006 T008, T045). 글 모듈이 채운다.
 * 위 모듈(comment, stats)은 글 표를 직접 읽지 않고 이것으로만 묻는다.
 */
public interface PostSummaryQuery {

    /** 블로그의 모든 글 번호 (비공개 포함). */
    List<Long> postIdsOf(Long blogId);

    /** 글 번호마다 제목. 없는 번호는 빠진다. */
    Map<Long, String> titlesOf(Collection<Long> postIds);

    /** 블로그의 최근 글 size개 (비공개 포함), (작성 시각, 글 번호) 최신순 (대시보드, FR-009). */
    List<PostSummary> recent(Long blogId, int size);

    /**
     * 받은 글 중 누구나 볼 수 있는 글(글 공개 그리고 분류 공개, 글 상세와 같은 조건)만 (인기 글, FR-008).
     * 비공개 분류에 든 공개 글은 빠진다 (2026-10-08 결정). 순서는 정하지 않는다.
     */
    List<PostSummary> publicAmong(Collection<Long> postIds);

    /** visibility는 "public" / "private". */
    record PostSummary(Long postId, String title, Instant createdAt, String visibility) {
    }
}
