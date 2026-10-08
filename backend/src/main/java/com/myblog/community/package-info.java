/**
 * 좋아요·신고 모듈: 글 좋아요(US3), 글 신고(US6) (specs/005).
 * 글은 글 모듈의 맨 위 타입(PostLookup, PostDeletingEvent)으로만 묻는다. 글 모듈은 이 모듈을 부르지 않는다
 * (user ← blog ← post ← community). 글 상세의 좋아요 수는 글 모듈의 틀(PostLikeSummary)을 이 모듈이 채운다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "좋아요·신고", allowedDependencies = {"post", "user", "common"})
package com.myblog.community;
