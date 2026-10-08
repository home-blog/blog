package com.myblog.user.security;

import com.myblog.user.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Security의 인증 이벤트를 받아 연속 실패 횟수를 센다.
 * 이벤트는 로그인 요청 안에서 바로(같은 스레드) 전달되므로, 5번째로 틀린 그 요청의 응답에 잠금을 알릴 수 있다 (US3-1).
 */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService attempts;

    public LoginAttemptListener(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof String email) {
            attempts.recordFailure(email);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof MemberPrincipal member) {
            attempts.recordSuccess(member.getId());
        }
    }
}
