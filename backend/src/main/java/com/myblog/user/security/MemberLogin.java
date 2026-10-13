package com.myblog.user.security;

import com.myblog.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 확인 없이 회원을 세션에 로그인시킨다: 소셜 로그인·소셜 가입 (specs/010).
 * 이메일 로그인(LoginService)과 같은 세션 모양(MemberPrincipal, 로그인 시각)을 남겨서, 그 뒤로는 둘을 구별하지 않는다.
 */
@Component
public class MemberLogin {

    private final SecurityContextRepository securityContextRepository;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public MemberLogin(SecurityContextRepository securityContextRepository, Clock clock) {
        this.securityContextRepository = securityContextRepository;
        this.clock = clock;
    }

    /** 새 로그인: 세션 ID를 바꾸고(FR-032) 로그인 시각을 새로 적는다(30일 상한의 시작). */
    public void logIn(User user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        save(user, request, response);
        request.getSession().setAttribute(SessionLifetimeFilter.LOGIN_AT, clock.millis());
    }

    /** 이미 로그인한 회원을 그대로 다시 적는다 (연결하기를 마친 뒤). 로그인 시각은 그대로 둔다. */
    public void restore(User user, HttpServletRequest request, HttpServletResponse response) {
        save(user, request, response);
    }

    /** 로그인 정보를 지운다 (회사 로그인은 마쳤지만 아직 회원이 아닌 사람). 세션의 다른 값은 남긴다. */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext empty = contextHolder.createEmptyContext();
        contextHolder.clearContext();
        securityContextRepository.saveContext(empty, request, response);
    }

    private void save(User user, HttpServletRequest request, HttpServletResponse response) {
        MemberPrincipal principal = MemberPrincipal.of(user, Instant.now(clock));
        principal.eraseCredentials();
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
