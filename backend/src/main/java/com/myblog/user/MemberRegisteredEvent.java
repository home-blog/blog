package com.myblog.user;

/**
 * 회원이 새로 가입했다 (Spring 이벤트). 가입과 같은 트랜잭션 안에서 바로 전달된다.
 * 다른 모듈은 user 모듈 안쪽을 직접 부르지 않고 이 이벤트를 받아 일한다 (예: blog가 블로그를 만든다, FR-011).
 */
public record MemberRegisteredEvent(Long memberId, String nickname) {
}
