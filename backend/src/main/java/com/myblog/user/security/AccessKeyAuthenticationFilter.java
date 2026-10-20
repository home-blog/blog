package com.myblog.user.security;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import com.myblog.user.AccessKeyGrant;
import com.myblog.user.service.AccessKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * MCP 주소 전용: {@code Authorization: Bearer gk_…} 열쇠를 확인해 그 회원으로 로그인시킨다 (specs/020 D-2).
 * <ul>
 *   <li>세션을 만들지도 읽지도 않는다(이 요청 동안만의 로그인).</li>
 *   <li>Origin 머리글이 있고 요청이 들어온 호스트와 다르면 403: 다른 사이트의 웹 페이지가 부르는 요청을 막는다(MCP 권고).
 *       AI 앱(MCP 클라이언트)은 Origin을 보내지 않는다. DNS 리바인딩은 Origin과 Host가 같은 공격자 이름이라 이 검사로는
 *       못 막는다: 그쪽은 브라우저가 모르는 열쇠(Bearer)가 있어야 한다는 점이 막는다.</li>
 *   <li>열쇠가 틀리면 로그인시키지 않고 넘긴다: 보안 사슬이 401과 {@code WWW-Authenticate: Bearer}로 답한다.</li>
 * </ul>
 */
public class AccessKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final AccessKeyService accessKeys;
    private final Clock clock;

    public AccessKeyAuthenticationFilter(AccessKeyService accessKeys, Clock clock) {
        this.accessKeys = accessKeys;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isCrossSite(request)) {
            JsonErrorWriter.write(response, ErrorCode.FORBIDDEN);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            accessKeys.verify(header.substring(BEARER.length()).strip()).ifPresent(verified -> {
                MemberPrincipal principal = MemberPrincipal.of(verified.user(), Instant.now(clock));
                principal.eraseCredentials();
                AccessKeyGrant grant = verified.grant();
                var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(
                        new SimpleGrantedAuthority("ROLE_MEMBER"),
                        new SimpleGrantedAuthority("ACCESS_KEY_" + grant.scope().toUpperCase())));
                authentication.setDetails(grant);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }

    /** Origin이 있고, 그 호스트(포트 포함)가 이 요청이 들어온 호스트와 다르면 다른 사이트의 브라우저 요청이다. */
    static boolean isCrossSite(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return false;
        }
        String host = request.getHeader("Host");
        try {
            URI uri = URI.create(origin.strip());
            String originHost = uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
            return host == null || originHost == null || !originHost.equalsIgnoreCase(host);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
