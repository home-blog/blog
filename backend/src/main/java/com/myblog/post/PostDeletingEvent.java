package com.myblog.post;

/**
 * 글을 지운다 (specs/003 T032, FR-022, data-model 5). 글 모듈이 글을 지우기 <b>전에</b>, 같은 트랜잭션 안에서 낸다.
 * 팀 ERD에 자동 연쇄 삭제가 없어서, 글에 딸린 표를 가진 모듈이 {@code @EventListener}로 같은 트랜잭션 안에서
 * 자기 표를 먼저 지운다. 하나라도 실패하면 글 삭제 전체가 취소된다 (한 묶음, SC-007).
 * 이어 붙일 곳 (005):
 * <ul>
 *   <li>comment: 이 글의 댓글(대댓글 포함), 댓글 신고 (D-7)</li>
 *   <li>community: 좋아요, 글 신고 (D-7)</li>
 *   <li>post: 태그 연결 (태그 자체는 남긴다)</li>
 *   <li>image: 이미지 기록. 이미지 파일은 트랜잭션이 끝난 뒤 지운다 (research R-1)</li>
 * </ul>
 * 지금은 듣는 쪽이 없다.
 */
public record PostDeletingEvent(Long postId) {
}
