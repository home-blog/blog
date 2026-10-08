package com.myblog.user.service;

import com.myblog.user.domain.User;

/**
 * 회원이 새로 만들어진 직후, 같은 트랜잭션 안에서 불린다.
 * 다른 모듈(blog)이 이 인터페이스를 구현해 "가입하면 블로그가 함께 생긴다"(FR-011)를 맡는다.
 * 모듈이 부르는 방향(user ← blog)을 지키기 위해 user는 blog를 직접 부르지 않는다.
 */
public interface MemberCreatedHandler {

    void onMemberCreated(User member);
}
