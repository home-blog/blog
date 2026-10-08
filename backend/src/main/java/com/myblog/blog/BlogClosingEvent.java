package com.myblog.blog;

/**
 * 블로그를 닫는다 (회원 탈퇴, specs/002 T032). 블로그 모듈이 블로그와 분류를 지우기 <b>전에</b> 낸다.
 * 아래 모듈은 {@code @EventListener}로 같은 트랜잭션 안에서 자기 표를 먼저 지운다. 팀 ERD에 자동 연쇄 삭제가 없어서 자식부터 지운다.
 * 이어 붙일 곳 (002 T035):
 * <ul>
 *   <li>003 post: 이 블로그의 글, 태그 연결, 이미지 기록, 글 신고. 이미지 파일은 트랜잭션이 끝난 뒤 지운다 (002 research R-7)</li>
 *   <li>005 comment: 이 블로그 글의 댓글·좋아요·댓글 신고</li>
 *   <li>006 stats: 이 블로그의 일별 통계</li>
 * </ul>
 */
public record BlogClosingEvent(Long blogId, Long ownerId) {
}
