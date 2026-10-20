package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.LoggedInMember;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LoggedInMember를 세션의 MemberPrincipal과 회원 표로 채운다 (CurrentMemberService와 같은 확인).
 * 탈퇴했거나 지금 정지 중인 회원(specs/022 S2-3)은 로그인하지 않은 것으로 본다.
 */
@Component
public class LoggedInMemberAdapter implements LoggedInMember {

    private final UserRepository users;
    private final Clock clock;

    public LoggedInMemberAdapter(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> idOf(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            return Optional.empty();
        }
        return users.findLoggedInById(principal.getId(), Instant.now(clock)).map(user -> user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Long requireIdOf(Authentication authentication) {
        return idOf(authentication).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }

    @Override
    @Transactional(readOnly = true)
    public Long requireAdminIdOf(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        User user = users.findLoggedInById(principal.getId(), Instant.now(clock))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        if (!user.isAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return user.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(Long memberId) {
        return memberId != null && users.findLoggedInById(memberId, Instant.now(clock)).map(User::isAdmin).orElse(false);
    }
}
