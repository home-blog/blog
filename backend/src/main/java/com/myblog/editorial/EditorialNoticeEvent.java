package com.myblog.editorial;

/**
 * 편집실이 글을 내리거나 지웠다 (specs/018 US1-4, US2-2). 구독 모듈이 커밋 뒤 듣고 쓴 사람의 우편함에 사유와 함께 소식을 넣는다.
 * postId는 내린 블로그 글(주인은 계속 볼 수 있다), 지운 글마당 글이면 null.
 */
public record EditorialNoticeEvent(Long memberId, Long postId, String title, String reason) {
}
