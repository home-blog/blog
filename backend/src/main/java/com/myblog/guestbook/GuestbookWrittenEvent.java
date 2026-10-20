package com.myblog.guestbook;

/**
 * 방명록에 새 엽서가 꽂혔다 (specs/021 US1-5). 구독 모듈이 커밋 뒤에 듣고 블로그 주인의 우편함에 소식을 넣는다.
 * 비밀 엽서여도 소식에는 내용을 싣지 않으므로 그대로 낸다.
 */
public record GuestbookWrittenEvent(Long entryId, Long blogId, String blogName, Long ownerId, Long writerId) {
}
