package com.myblog.user.security;

import com.myblog.user.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 로그인 유지 기간 (specs/001 T027, T029, FR-030).
 * - 로그인한 때부터 30일이 지나면, 계속 쓰고 있어도 로그인을 끝낸다 (세션 삭제 + 쿠키 만료).
 * - 쓸 때마다 세션 쿠키를 다시 내려 보내 브라우저의 쿠키 만료(7일)도 뒤로 민다 (research R-1:
 *   Spring Session은 세션 ID가 바뀔 때만 쿠키를 쓰므로, 그대로 두면 처음 로그인하고 7일 뒤 브라우저가 쿠키를 버린다).
 * "마지막 사용 후 7일"은 Spring Session이 지킨다 (server.servlet.session.timeout).
 */
public class SessionLifetimeFilter extends OncePerRequestFilter {

    /** 로그인한 시각(밀리초)을 담는 세션 속성 이름. */
    public static final String LOGIN_AT = "myblog.loginAt";

    private final CookieSerializer cookieSerializer;
    private final AuthProperties properties;
    private final Clock clock;

    public SessionLifetimeFilter(CookieSerializer cookieSerializer, AuthProperties properties, Clock clock) {
        this.cookieSerializer = cookieSerializer;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LOGIN_AT) instanceof Long loginAt) {
            if (clock.millis() - loginAt > properties.session().absoluteTimeout().toMillis()) {
                session.invalidate();
                cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));
            } else {
                cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, session.getId()));
            }
        }
        chain.doFilter(request, response);
    }
}
