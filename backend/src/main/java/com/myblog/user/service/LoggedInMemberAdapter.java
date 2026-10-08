package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.LoggedInMember;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** LoggedInMember를 세션의 MemberPrincipal과 회원 표로 채운다 (CurrentMemberService와 같은 확인). */
@Component
public class LoggedInMemberAdapter implements LoggedInMember {

    private final UserRepository users;

    public LoggedInMemberAdapter(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> idOf(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            return Optional.empty();
        }
        return users.findActiveById(principal.getId()).map(user -> user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Long requireIdOf(Authentication authentication) {
        return idOf(authentication).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }
}
