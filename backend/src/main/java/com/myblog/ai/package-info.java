/**
 * AI 모듈: 빨간 펜 교정 (specs/014 US1).
 * 글 본문을 받아 AI에게 교정을 받고, 서버가 결과를 다시 검사해 돌려준다. 글을 저장하거나 읽지 않는다.
 * 로그인한 회원은 회원 모듈의 맨 위 틀(LoggedInMember)로만 묻는다 (user ← ai). 다른 모듈은 이 모듈을 부르지 않는다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI", allowedDependencies = {"user", "common"})
package com.myblog.ai;
