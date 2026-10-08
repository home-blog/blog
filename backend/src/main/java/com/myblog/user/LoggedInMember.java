package com.myblog.user;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * "이 요청을 보낸 회원의 번호"를 다른 모듈에 알려 주는 틀 (specs/003 T007, FR-008, FR-044).
 * 세션 속 회원(MemberPrincipal)은 회원 모듈 안쪽이라 블로그·글 모듈이 직접 쓰지 않고 이것만 쓴다.
 * 회원 번호는 세션에서만 얻는다. 주소나 요청 본문에서 받지 않는다. 탈퇴했는지는 DB를 다시 본다 (002 research B-1).
 */
public interface LoggedInMember {

    /** 누구나 부르는 읽기 주소용: 로그인하지 않았거나 탈퇴한 회원이면 비운다. */
    Optional<Long> idOf(Authentication authentication);

    /** 로그인해야 하는 주소용: 로그인하지 않았거나 탈퇴한 회원이면 401 UNAUTHENTICATED. */
    Long requireIdOf(Authentication authentication);
}
