/**
 * 구독·알림 모듈 (specs/016): 원고 받아 보기(블로그 구독), 우편함(새 글·댓글 소식), 원고 편지(새 글 메일).
 * 다른 모듈의 맨 위 타입(입구 틀, 이벤트)만 부른다. 아래 모듈은 이 모듈을 부르지 않는다: 새 글·댓글은 그 모듈이 이벤트로 알리고,
 * 블로그 머리의 받아 보는 수(BlogSubscriptionSummary)와 받아 보는 글의 블로그 번호(SubscribedBlogs)는 아래 모듈의 틀을 이 모듈이 채운다
 * (user ← blog ← post ← comment ← subscription). 글마당 댓글·채택 소식도 글마당 모듈의 이벤트로 받는다(specs/017 D-9).
 * 편집실이 글을 내리거나 지운 소식은 편집실 모듈의 EditorialNoticeEvent로 받는다(specs/018).
 * 방명록의 새 엽서·답장 소식은 방명록 모듈의 GuestbookWrittenEvent·GuestbookRepliedEvent로 받는다(specs/021).
 */
@org.springframework.modulith.ApplicationModule(displayName = "구독·알림",
        allowedDependencies = {"guestbook", "editorial", "board", "comment", "post", "blog", "user", "common"})
package com.myblog.subscription;
