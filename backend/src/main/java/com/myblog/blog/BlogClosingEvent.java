package com.myblog.blog;

/**
 * 블로그를 닫는다 (회원 탈퇴, specs/002 T032). 블로그 모듈이 블로그와 분류를 지우기 <b>전에</b> 낸다.
 * 아래 모듈은 {@code @EventListener}로 같은 트랜잭션 안에서 자기 표를 먼저 지운다. 팀 ERD에 자동 연쇄 삭제가 없어서 자식부터 지운다.
 * 이어 붙일 곳 (002 T035):
 * <ul>
 *   <li>003 post: 이 블로그의 글 (PostBlogClosingCleaner). 글마다 PostDeletingEvent를 내므로
 *       005의 댓글·좋아요·글 신고·태그 연결·이미지 기록은 그 이벤트로 함께 지워진다 (005는 이 이벤트를 따로 듣지 않는다).
 *       댓글 신고는 쓰지 않는다 (005 D-8)</li>
 *   <li>006 stats: 이 블로그의 일별 통계</li>
 * </ul>
 */
public record BlogClosingEvent(Long blogId, Long ownerId) {
}
