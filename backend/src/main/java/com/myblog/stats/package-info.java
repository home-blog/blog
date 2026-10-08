/**
 * 통계·블로그 관리 모듈 (specs/006): 블로그 관리 화면의 머리 정보·대시보드·통계, 조회수·방문자 세기.
 * 맨 위 모듈이라 다른 모듈의 맨 위 타입(입구 틀, 이벤트)만 부르고, 다른 모듈의 표에는 직접 쓰지 않는다.
 * 아래 모듈은 이 모듈을 부르지 않는다: 글을 읽었다는 것은 글 모듈이 이벤트로 알리고 이 모듈이 듣는다
 * (user ← blog ← post ← comment ← stats).
 */
@org.springframework.modulith.ApplicationModule(displayName = "통계·블로그 관리",
        allowedDependencies = {"comment", "post", "blog", "user", "common"})
package com.myblog.stats;
