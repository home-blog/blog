package com.myblog.user.security;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security가 로그인할 때 회원을 찾는 곳. 로그인 칸에 넣은 이메일로, 탈퇴하지 않은 회원에서 찾는다 (FR-024).
 * 없으면 UsernameNotFoundException → Spring Security가 "비밀번호 틀림"과 같은 오류로 바꿔 계정 존재를 숨긴다 (FR-026).
 */
@Service
public class MemberDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final Clock clock;

    public MemberDetailsService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberPrincipal loadUserByUsername(String email) {
        User user = users.findActiveByEmail(User.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("회원 없음"));
        return MemberPrincipal.of(user, Instant.now(clock));
    }
}
