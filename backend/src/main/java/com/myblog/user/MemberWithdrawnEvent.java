package com.myblog.user;

/**
 * 회원이 탈퇴했다 (specs/002 T030, FR-024). 회원 모듈이 탈퇴 트랜잭션 안에서 낸다.
 * 듣는 쪽은 {@code @EventListener}로 <b>같은 트랜잭션 안에서 바로</b> 자기 데이터를 지운다. 하나라도 실패하면 탈퇴 전체가 취소된다.
 * 탈퇴 뒤에 따로 도는 {@code @TransactionalEventListener}·{@code @ApplicationModuleListener}는 쓰지 않는다 (묶음이 깨진다).
 * 지금 듣는 쪽: blog(BlogWithdrawalCleaner). 005 community(LikeCleaner)가 "내가 누른 좋아요"를 지운다.
 * 005 comment는 듣지 않는다: 회원 줄은 남아서 남의 글에 단 댓글이 "탈퇴한 사용자"로 보인다 (D-1).
 * 내 블로그 글의 댓글은 글이 지워질 때 PostDeletingEvent로 함께 지워진다. 내가 한 신고는 남긴다 (D-5).
 */
public record MemberWithdrawnEvent(Long memberId) {
}
