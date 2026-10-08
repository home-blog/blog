package com.myblog.comment;

/**
 * 새 댓글 수 계산 한곳 (specs/006 T033, FR-027, FR-028, SC-008). 메뉴 옆·사용자 메뉴·대시보드가 모두 이것만 부른다.
 * 새 댓글 = 이 회원 블로그의 글에 달린 댓글 중 댓글 관리를 마지막으로 연 시각보다 늦고(한 번도 안 열었으면 모두)
 * 블로그 주인이 쓰지 않은 것.
 */
public interface NewCommentCounter {

    /** 이 회원 블로그의 새 댓글 수. */
    long countFor(Long memberId);
}
