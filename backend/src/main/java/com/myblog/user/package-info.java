/**
 * 회원 모듈: 가입, 이메일 인증, 로그인, 계정 (specs/001, 002).
 * 다른 모듈에 내보내는 것은 이 패키지 바로 아래의 타입(예: MemberRegisteredEvent)뿐이다. 하위 패키지는 모듈 안쪽이다.
 * 회원 모듈은 맨 아래 모듈이라 공통(common)만 부를 수 있다 (헌법: user ← blog ← post …).
 */
@org.springframework.modulith.ApplicationModule(displayName = "회원", allowedDependencies = "common")
package com.myblog.user;
