package com.myblog.blog;

/**
 * 블로그의 방문자용 화면을 보여 줬다 (specs/006 T056, D-5 B, FR-034). 블로그 모듈이 블로그 정보(GET /api/blogs/{blogId},
 * 블로그 첫 화면과 분류별 목록이 열 때마다 부른다)를 줄 때 낸다. 통계 모듈이 듣고 <b>방문자만</b> 센다.
 * 글 상세의 방문은 PostViewedEvent로 함께 센다.
 *
 * 읽기 트랜잭션이 끝난 뒤(컨트롤러에서) 낸다 (PostViewedEvent와 같은 까닭).
 *
 * @param viewerId 로그인하지 않았으면 null
 * @param byOwner 블로그 주인이 본 것이면 참 (주인은 세지 않는다, D-4)
 */
public record BlogVisitedEvent(Long blogId, Long viewerId, boolean byOwner) {
}
