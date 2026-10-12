package com.myblog.user.controller;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberLogin;
import com.myblog.user.service.SocialAccountService;
import com.myblog.user.social.SocialClientRegistrations;
import com.myblog.user.social.SocialProvider;
import com.myblog.user.social.SocialSessionState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 로그인 버튼과 소셜 가입 (specs/010 contracts 1, 3 ~ 5). 로그인 없이 부른다 (SecurityConfig의 /api/*\/auth/**).
 * 회사 화면 오가기는 Spring Security가 /oauth2/authorization/{회사}, /login/oauth2/code/{회사}에서 한다 (SocialLoginHandlers).
 */
@RestController
@RequestMapping("/api/{version}/auth/social")
public class SocialAuthController {

    private final SocialClientRegistrations registrations;
    private final SocialAccountService socialAccounts;
    private final MemberLogin memberLogin;
    private final Clock clock;

    public SocialAuthController(SocialClientRegistrations registrations, SocialAccountService socialAccounts,
            MemberLogin memberLogin, Clock clock) {
        this.registrations = registrations;
        this.socialAccounts = socialAccounts;
        this.memberLogin = memberLogin;
        this.clock = clock;
    }

    /** 열쇠가 들어 있는 회사만 (D-9). */
    @GetMapping("/providers")
    public ProvidersResponse providers() {
        return new ProvidersResponse(registrations.enabled().stream().map(SocialProvider::id).toList());
    }

    @GetMapping("/pending")
    public PendingResponse pending(HttpServletRequest request) {
        SocialSessionState.Pending pending = requirePending(request);
        long seconds = Math.max(0, Duration.between(Instant.now(clock), pending.expiresAt()).toSeconds());
        return new PendingResponse(pending.provider().id(), pending.email(), pending.hasVerifiedEmail(), pending.nickname(),
                seconds);
    }

    @DeleteMapping("/pending")
    public ResponseEntity<Void> cancel(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SocialSessionState.PENDING);
        }
        return ResponseEntity.noContent().build();
    }

    /** 소셜 가입. 끝나면 바로 로그인된다 (D-3). */
    @PostMapping("/signup")
    public ResponseEntity<AuthController.MemberResponse> signup(@RequestBody SignupRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        SocialSessionState.Pending pending = requirePending(request);
        User member = socialAccounts.signup(pending, body.nickname(), body.email(), body.verificationToken());
        request.getSession().removeAttribute(SocialSessionState.PENDING);
        memberLogin.logIn(member, request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthController.MemberResponse(new AuthController.Member(member.getId(), member.getNickname())));
    }

    private SocialSessionState.Pending requirePending(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SocialSessionState.PENDING);
        if (!(value instanceof SocialSessionState.Pending pending) || pending.expired(Instant.now(clock))) {
            if (session != null) {
                session.removeAttribute(SocialSessionState.PENDING);
            }
            throw new ApiException(ErrorCode.SOCIAL_PENDING_NOT_FOUND);
        }
        return pending;
    }

    public record ProvidersResponse(List<String> providers) {
    }

    /** nickname은 제안(없으면 null). emailVerified가 참이면 email을 그대로 쓰고 이메일 인증을 건너뛴다. */
    public record PendingResponse(String provider, String email, boolean emailVerified, String nickname,
            long expiresInSeconds) {
    }

    /** email, verificationToken은 회사가 확인한 이메일이 없을 때만 쓴다 (contracts 4). */
    public record SignupRequest(String nickname, String email, String verificationToken) {
    }
}
