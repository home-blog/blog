package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberPrincipal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 속 회원을 DB에서 다시 확인한다 (specs/002 research B-1, FR-002, FR-029).
 * 세션에는 로그인한 때의 값이 남아 있으므로, 닉네임처럼 바뀌는 값과 "탈퇴했는지"는 DB를 본다.
 * 회원 번호는 세션에서만 얻는다. 주소나 요청 본문에서 받지 않는다.
 */
@Service
public class CurrentMemberService {

    private final UserRepository users;
    private final Clock clock;

    public CurrentMemberService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    /** 로그인하지 않았거나 이미 탈퇴했거나 지금 정지 중이면(specs/022 S2-3) 401 UNAUTHENTICATED. */
    @Transactional(readOnly = true)
    public User get(MemberPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        return users.findLoggedInById(principal.getId(), Instant.now(clock))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }
}
