/**
 * 구독·알림 모듈 (specs/016): 원고 받아 보기(블로그 구독), 우편함(새 글·댓글 소식), 원고 편지(새 글 메일).
 * 다른 모듈의 맨 위 타입(입구 틀, 이벤트)만 부른다. 아래 모듈은 이 모듈을 부르지 않는다: 새 글·댓글은 그 모듈이 이벤트로 알리고,
 * 블로그 머리의 받아 보는 수(BlogSubscriptionSummary)와 받아 보는 글의 블로그 번호(SubscribedBlogs)는 아래 모듈의 틀을 이 모듈이 채운다
 * (user ← blog ← post ← comment ← subscription).
 */
@org.springframework.modulith.ApplicationModule(displayName = "구독·알림",
        allowedDependencies = {"comment", "post", "blog", "user", "common"})
package com.myblog.subscription;
