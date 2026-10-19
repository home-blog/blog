package com.myblog.post;

/**
 * 글이 <b>처음</b> 공개됐다 (specs/016 D-2): 공개 분류에 공개 글로 처음 저장된 순간(바로 발행, 예약 발행, 비공개였다가 처음 공개로 바꿈).
 * 글 모듈이 글을 저장한 같은 트랜잭션 안에서 내고, 한 글에 한 번뿐이다(post.first_published_at).
 * 듣는 쪽(subscription)은 트랜잭션이 끝난 뒤(AFTER_COMMIT) 받아 보는 사람에게 소식·메일을 보낸다. 글 저장은 그 결과를 기다리지 않는다.
 *
 * @param content 마크다운 본문 (메일의 첫 문단 요약을 만든다)
 */
public record PostPublishedEvent(Long postId, Long blogId, Long ownerId, String blogName, String title, String content) {
}
