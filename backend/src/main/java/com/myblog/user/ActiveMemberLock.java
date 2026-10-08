package com.myblog.user;

/**
 * 회원 줄을 잠가 "아직 탈퇴하지 않았다"를 트랜잭션 끝까지 지키는 틀 (specs/005 PR #30 리뷰).
 * 탈퇴는 회원 줄을 FOR UPDATE로 잠근 채 MemberWithdrawnEvent로 그 회원의 데이터를 지운다 (WithdrawalService).
 * 다른 모듈이 "그 회원의 줄을 새로 넣는" 일(예: 좋아요)을 할 때 이것으로 먼저 잠그면, 탈퇴와 겹쳐도
 * 탈퇴 정리 뒤에 줄이 남지 않는다: 탈퇴가 끝날 때까지 기다렸다가 탈퇴한 회원이면 거짓을 받는다.
 */
public interface ActiveMemberLock {

    /** 회원 줄을 공유 잠금으로 잡는다 (트랜잭션 안에서 불러야 한다). 탈퇴했거나 없는 회원이면 거짓. */
    boolean lockIfActive(Long memberId);
}
