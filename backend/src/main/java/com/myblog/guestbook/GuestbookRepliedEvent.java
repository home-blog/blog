package com.myblog.guestbook;

/**
 * 블로그 주인이 엽서에 처음 답장했다 (specs/021 US2-2, S2-2). 구독 모듈이 커밋 뒤에 듣고 엽서를 쓴 사람의 우편함에 소식을 넣는다.
 * 답장을 고칠 때는 내지 않는다.
 */
public record GuestbookRepliedEvent(Long entryId, Long blogId, String blogName, Long ownerId, Long writerId) {
}
