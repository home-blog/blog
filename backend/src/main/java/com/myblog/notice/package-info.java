/**
 * 공지사항 모듈: 업데이트 소식과 공지 (specs/012).
 * 관리자 확인은 회원 모듈의 맨 위 틀(LoggedInMember)로만 묻는다 (user ← notice). 다른 모듈은 이 모듈을 부르지 않는다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "공지사항", allowedDependencies = {"user", "common"})
package com.myblog.notice;
