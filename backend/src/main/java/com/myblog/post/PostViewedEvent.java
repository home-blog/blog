package com.myblog.post;

/**
 * 글 상세를 보여 줬다 (specs/006 T051, contracts 9, FR-033). 글 모듈이 <b>글을 보여 줄 수 있을 때만</b> 낸다
 * (없는 글·볼 수 없는 글은 내지 않음). 통계 모듈이 듣고 조회수·방문자를 센다. 글 모듈은 통계 모듈을 부르지 않는다.
 * 듣는 쪽의 실패는 글 읽기를 망치지 않는다 (듣는 쪽이 삼킨다, NF-09).
 *
 * @param viewerId 로그인하지 않았으면 null
 */
public record PostViewedEvent(Long postId, Long blogId, Long ownerId, Long viewerId) {
}
