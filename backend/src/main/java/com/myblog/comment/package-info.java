/**
 * 댓글 모듈: 글의 댓글 쓰기·읽기·삭제 (specs/005 US1, US2).
 * 글은 글 모듈의 맨 위 타입(PostLookup, PostDeletingEvent)으로만 묻고, 회원 이름은 회원 모듈의 MemberNames로 읽는다.
 * 글 모듈은 이 모듈을 부르지 않는다 (user ← blog ← post ← comment). 글 상세의 댓글 수는 글 모듈의 틀(PostCommentCounter)을 이 모듈이 채운다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "댓글", allowedDependencies = {"post", "user", "common"})
package com.myblog.comment;
