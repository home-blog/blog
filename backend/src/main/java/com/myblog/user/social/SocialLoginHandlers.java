package com.myblog.user.social;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberLogin;
import com.myblog.user.service.SocialAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 회사 화면에서 돌아온 뒤 (specs/010 T004, contracts 2). 회사 로그인 정보(SocialOAuth2User)는 세션에 남기지 않고,
 * 우리 회원으로 로그인시키거나(MemberPrincipal) 지운 뒤 화면 주소로 보낸다.
 */
@Component
public class SocialLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginHandlers.class);

    static final String HOME = "/";
    static final String SOCIAL_SIGNUP = "/signup/social";
    static final String LOGIN_FAILED = "/login?socialError=" + ErrorCode.SOCIAL_LOGIN_FAILED.name();

    private final SocialAccountService socialAccounts;
    private final UserRepository users;
    private final MemberLogin memberLogin;
    private final Clock clock;
    private final RedirectStrategy redirect = new DefaultRedirectStrategy();

    public SocialLoginHandlers(SocialAccountService socialAccounts, UserRepository users, MemberLogin memberLogin, Clock clock) {
        this.socialAccounts = socialAccounts;
        this.users = users;
        this.memberLogin = memberLogin;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        try {
            handleSuccess(request, response, authentication);
        } catch (RuntimeException e) {
            // 필터가 회사 로그인 정보를 이미 세션에 적었다. 그대로 두면 회원이 아닌 로그인이 남으므로 지우고 실패로 보낸다
            log.warn("소셜 로그인 처리 실패", e);
            memberLogin.clear(request, response);
            redirect.sendRedirect(request, response, LOGIN_FAILED);
        }
    }

    private void handleSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        SocialProfile profile = ((SocialOAuth2User) authentication.getPrincipal()).profile();
        HttpSession session = request.getSession();
        SocialSessionState.LinkIntent intent = (SocialSessionState.LinkIntent) session.getAttribute(SocialSessionState.LINK);
        session.removeAttribute(SocialSessionState.LINK);
        session.removeAttribute(SocialSessionState.PENDING);
        Instant now = Instant.now(clock);

        if (intent != null) {
            // 연결하기 중이었다: 시간이 지났어도 일반 로그인으로 넘어가지 않는다 (지금 로그인한 회원이 바뀌지 않게)
            linkFor(intent, profile, intent.expired(now), request, response);
            return;
        }
        switch (socialAccounts.signIn(profile)) {
            case SocialAccountService.SignIn.Member member -> {
                memberLogin.logIn(member.user(), request, response);
                redirect.sendRedirect(request, response, HOME);
            }
            case SocialAccountService.SignIn.Pending pending -> {
                memberLogin.clear(request, response);
                session.setAttribute(SocialSessionState.PENDING, pending.pending());
                redirect.sendRedirect(request, response, SOCIAL_SIGNUP);
            }
        }
    }

    /** US4 연결하기: 연결을 시도하고, 회사 로그인으로 바뀐 세션의 로그인 정보를 원래 회원으로 되돌린다. */
    private void linkFor(SocialSessionState.LinkIntent intent, SocialProfile profile, boolean expired,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<User> member = users.findActiveById(intent.memberId());
        if (member.isEmpty()) {
            memberLogin.clear(request, response);
            redirect.sendRedirect(request, response, LOGIN_FAILED);
            return;
        }
        String target;
        try {
            if (expired || intent.provider() != profile.provider()) {
                throw new ApiException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
            socialAccounts.link(member.get().getId(), profile);
            target = "/mypage?linked=" + profile.provider().id();
        } catch (ApiException e) {
            target = "/mypage?linkError=" + e.errorCode().name();
        }
        memberLogin.restore(member.get(), request, response);
        redirect.sendRedirect(request, response, target);
    }

    /** 회사 화면에서 취소했거나 오류. 연결하기 중이었으면 마이페이지로(로그인은 그대로), 아니면 로그인 창으로. */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.info("소셜 로그인 실패: {}", exception.getMessage());
        HttpSession session = request.getSession(false);
        boolean linking = session != null && session.getAttribute(SocialSessionState.LINK) != null;
        if (session != null) {
            session.removeAttribute(SocialSessionState.LINK);
        }
        redirect.sendRedirect(request, response,
                linking ? "/mypage?linkError=" + ErrorCode.SOCIAL_LOGIN_FAILED.name() : LOGIN_FAILED);
    }
}
