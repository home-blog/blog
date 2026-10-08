/**
 * 글 모듈: 글 쓰기·읽기·수정·삭제, 주제 목록 (specs/003).
 * 블로그·분류는 블로그 모듈의 맨 위 타입(BlogDirectory 등)으로만 묻는다. 블로그 모듈은 이 모듈을 부르지 않는다
 * (헌법: user ← blog ← post …). 블로그 모듈이 묻는 "분류의 글 개수"는 블로그 모듈의 틀(CategoryPostCounter)을 이 모듈이 채운다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "글", allowedDependencies = {"blog", "user", "common"})
package com.myblog.post;
