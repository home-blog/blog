package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.security.SessionLifetimeFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * 로그인 (specs/001 contracts 6, FR-024 ~ FR-032).
 * 비밀번호 비교와 잠금 확인은 Spring Security(AuthenticationManager)가 하고, 여기서는 결과를 계약대로 답하고 세션에 저장한다.
 */
@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptService attempts;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public LoginService(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
            LoginAttemptService attempts, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.attempts = attempts;
        this.clock = clock;
    }

    public MemberPrincipal login(String rawEmail, String password, HttpServletRequest request, HttpServletResponse response) {
        String email = User.normalizeEmail(rawEmail);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (LockedException e) {
            // 잠겨 있으면 비밀번호를 비교하지 않고 거절 (FR-027)
            throw locked(email);
        } catch (BadCredentialsException e) {
            // 이메일이 없든 비밀번호가 틀리든 같은 응답 (FR-026). 단, 방금 5번째로 틀려 잠겼다면 잠금을 알린다 (US3-1)
            if (attempts.lockedForSeconds(email).isPresent()) {
                throw locked(email);
            }
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 새 로그인 상태: 세션이 있었다면 세션 ID를 바꿔 이전 값을 쓸 수 없게 한다 (FR-032)
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        }
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        request.getSession().setAttribute(SessionLifetimeFilter.LOGIN_AT, clock.millis());
        return (MemberPrincipal) authentication.getPrincipal();
    }

    private ApiException locked(String email) {
        return attempts.lockedError(email, ErrorCode.ACCOUNT_LOCKED.message());
    }
}
