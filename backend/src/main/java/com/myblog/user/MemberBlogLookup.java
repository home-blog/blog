package com.myblog.user;

import java.util.Optional;

/**
 * "이 회원의 블로그 번호"를 묻는 질문 틀 (specs/002 T012, FR-003).
 * 회원 모듈은 블로그 모듈을 직접 부르지 못하므로(user ← blog), 회원 모듈이 틀만 정하고 블로그 모듈이 채운다.
 */
public interface MemberBlogLookup {

    Optional<Long> blogIdOf(Long memberId);
}
