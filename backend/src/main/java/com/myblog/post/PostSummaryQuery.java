package com.myblog.post;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 블로그 관리(댓글 관리, 대시보드)가 글에 대해 묻는 입구 (specs/006 T008). 글 모듈이 채운다.
 * 위 모듈(comment, stats)은 글 표를 직접 읽지 않고 이것으로만 묻는다.
 */
public interface PostSummaryQuery {

    /** 블로그의 모든 글 번호 (비공개 포함). */
    List<Long> postIdsOf(Long blogId);

    /** 글 번호마다 제목. 없는 번호는 빠진다. */
    Map<Long, String> titlesOf(Collection<Long> postIds);
}
