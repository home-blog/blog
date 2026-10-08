package com.myblog.user.config;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import com.myblog.user.security.MemberDetailsService;
import com.myblog.user.security.SessionLifetimeFilter;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.session.web.http.CookieSerializer;

/**
 * 보안 설정 (specs/001 T011, T026 ~ T028, plan.md 헌법 IV).
 * <ul>
 *   <li>로그인: Spring Security의 AuthenticationManager(DaoAuthenticationProvider + BCrypt)로 확인한다.
 *       잠금은 UserDetails의 isAccountNonLocked로, 실패 횟수는 Security의 인증 이벤트로 센다 (LoginAttemptListener).</li>
 *   <li>로그인 상태는 서버 세션(Spring Session JDBC)에 저장한다. 로그인할 때 세션 ID를 새로 만든다 (FR-032).</li>
 *   <li>로그아웃: Security의 LogoutFilter가 POST /api/auth/logout에서 세션을 지우고 쿠키를 만료시킨 뒤 204 (contracts 7).</li>
 *   <li>변경 요청(POST 등)은 CSRF 토큰을 헤더(X-XSRF-TOKEN)에 실어야 한다. 토큰은 GET /api/auth/csrf로 받는다 (NF-11).</li>
 *   <li>로그인이 필요한 요청을 로그인 없이 보내면 401 + UNAUTHENTICATED (contracts 9).</li>
 *   <li><b>기본은 "로그인 필요"</b>다. 다른 기능에서 로그인 없이 열어 둘 주소는 아래 authorizeHttpRequests에 하나씩 적는다 (T035).</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
            CookieSerializer cookieSerializer, AuthProperties properties, Clock clock) throws Exception {
        // 빈으로 만들지 않는다: 빈이면 서블릿 필터로도 따로 등록돼 보안 필터 밖에서 한 번 더 돈다
        SessionLifetimeFilter sessionLifetimeFilter = new SessionLifetimeFilter(cookieSerializer, properties, clock);
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(sessionLifetimeFilter, SecurityContextHolderFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies(SessionConfig.COOKIE_NAME)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticated())
                        .accessDeniedHandler(accessDenied()))
                .authorizeHttpRequests(authorize -> authorize
                        // 로그인 없이 쓰는 주소 (001)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 누구나 읽는 블로그·글 (003 contracts 1, 2, 11). 비공개는 서비스가 주인인지 보고 거른다
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/{postId:\\d+}").permitAll()
                        // 공개 글 검색 (004 contracts 2). 누가 검색하든 공개 분류의 공개 글만 찾는다
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        // 그 밖은 모두 로그인 필요
                        .anyRequest().authenticated());
        return http.build();
    }

    /** 비밀번호는 BCrypt 해시로만 저장한다 (FR-010). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 이메일로 회원을 찾고(MemberDetailsService), BCrypt로 비밀번호를 비교한다. 결과는 이벤트로 알린다. */
    @Bean
    public AuthenticationManager authenticationManager(MemberDetailsService memberDetailsService,
            PasswordEncoder passwordEncoder, AuthenticationEventPublisher authenticationEventPublisher) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(memberDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager manager = new ProviderManager(provider);
        manager.setAuthenticationEventPublisher(authenticationEventPublisher);
        return manager;
    }

    /** 인증 성공·실패를 Spring 이벤트로 낸다 (실패 횟수 세기에 쓴다). */
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    /** 로그인 상태(SecurityContext)를 세션에 저장한다. 로그인 서비스와 필터가 같은 저장소를 쓴다. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private static AuthenticationEntryPoint unauthenticated() {
        return (request, response, exception) -> JsonErrorWriter.write(response, ErrorCode.UNAUTHENTICATED);
    }

    private static AccessDeniedHandler accessDenied() {
        return (request, response, exception) -> JsonErrorWriter.write(response,
                exception instanceof CsrfException ? ErrorCode.CSRF_TOKEN_INVALID : ErrorCode.FORBIDDEN);
    }
}
