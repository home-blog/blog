package com.myblog.user.config;

import com.myblog.common.ApiVersionConfig;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import com.myblog.user.security.MemberDetailsService;
import com.myblog.user.security.SessionLifetimeFilter;
import com.myblog.user.social.DiscardingAuthorizedClientRepository;
import com.myblog.user.social.KnownProviderAuthorizationRequestResolver;
import com.myblog.user.social.SocialClientRegistrations;
import com.myblog.user.social.SocialLoginHandlers;
import com.myblog.user.social.SocialUserService;
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
 *   <li>로그아웃: Security의 LogoutFilter가 POST /api/v1/auth/logout에서 세션을 지우고 쿠키를 만료시킨 뒤 204 (contracts 7).</li>
 *   <li>변경 요청(POST 등)은 CSRF 토큰을 헤더(X-XSRF-TOKEN)에 실어야 한다. 토큰은 GET /api/v1/auth/csrf로 받는다 (NF-11).</li>
 *   <li>로그인이 필요한 요청을 로그인 없이 보내면 401 + UNAUTHENTICATED (contracts 9).</li>
 *   <li>소셜 로그인(specs/010)은 oauth2Login이 회사 화면을 오가고, 돌아오면 같은 세션 로그인으로 바꾼다.</li>
 *   <li><b>기본은 "로그인 필요"</b>다. 다른 기능에서 로그인 없이 열어 둘 주소는 아래 authorizeHttpRequests에 하나씩 적는다 (T035).</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
            CookieSerializer cookieSerializer, AuthProperties properties, Clock clock,
            SocialClientRegistrations socialRegistrations, SocialUserService socialUserService,
            SocialLoginHandlers socialLoginHandlers) throws Exception {
        // 빈으로 만들지 않는다: 빈이면 서블릿 필터로도 따로 등록돼 보안 필터 밖에서 한 번 더 돈다
        SessionLifetimeFilter sessionLifetimeFilter = new SessionLifetimeFilter(cookieSerializer, properties, clock);
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        http
                // 버전이 없거나 모르는 버전의 API 주소는 답하는 처리기가 없으므로 CSRF 대신 버전 오류(400)로 답한다 (ApiVersionConfig)
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(ApiVersionConfig::isUnsupportedApiRequest))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(sessionLifetimeFilter, SecurityContextHolderFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // 소셜 로그인 (specs/010): /oauth2/authorization/{회사} → 회사 화면 → /login/oauth2/code/{회사}.
                // 회사 토큰은 저장하지 않고(D-7), 돌아오면 SocialLoginHandlers가 우리 회원 세션으로 바꾼다.
                // loginPage는 화면(React)의 로그인 창이다. 적어 두면 Spring의 기본 로그인 페이지가 생기지 않는다
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .clientRegistrationRepository(socialRegistrations)
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(new KnownProviderAuthorizationRequestResolver(socialRegistrations)))
                        .authorizedClientRepository(new DiscardingAuthorizedClientRepository())
                        .userInfoEndpoint(userInfo -> userInfo.userService(socialUserService))
                        .successHandler(socialLoginHandlers)
                        .failureHandler(socialLoginHandlers))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")   // 로그아웃은 필터가 받으므로 버전을 적어 둔다 (ApiVersionConfig)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies(SessionConfig.COOKIE_NAME)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticated())
                        .accessDeniedHandler(accessDenied()))
                .authorizeHttpRequests(authorize -> authorize
                        // 로그인 없이 쓰는 주소 (001)
                        .requestMatchers("/api/*/auth/**").permitAll()   // /api/{버전}/… (ApiVersionConfig)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // API 문서 (springdoc). 개발 설정에서만 켜지고 배포에는 없다 (OpenApiConfig)
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // 버전이 없거나 지원하지 않는 API 주소: 로그인과 상관없이 400 API_VERSION_UNSUPPORTED (ApiVersionConfig)
                        .requestMatchers(ApiVersionConfig::isUnsupportedApiRequest).permitAll()
                        // 누구나 읽는 블로그·글 (003 contracts 1, 2, 11). 비공개는 서비스가 주인인지 보고 거른다
                        .requestMatchers(HttpMethod.GET, "/api/*/blogs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/*/posts", "/api/*/posts/popular", "/api/*/posts/summary", "/api/*/posts/today-issue").permitAll()   // 첫 화면 글 모음·머리
                        .requestMatchers(HttpMethod.GET, "/api/*/posts/{postId:\\d+}").permitAll()
                        // 공개 글 검색 (004 contracts 2). 누가 검색하든 공개 분류의 공개 글만 찾는다
                        .requestMatchers(HttpMethod.GET, "/api/*/search/**").permitAll()
                        // 글의 댓글 목록 (005 contracts 1). 볼 수 없는 글이면 서비스가 없는 글과 같은 404로 답한다
                        .requestMatchers(HttpMethod.GET, "/api/*/posts/{postId:\\d+}/comments").permitAll()
                        // 같은 태그의 공개 글 목록 (005 contracts 9). 누가 보든 공개 분류의 공개 글만
                        .requestMatchers(HttpMethod.GET, "/api/*/tags/**").permitAll()
                        // 연재 읽기 (013 contracts 2-2, 2-3). 볼 수 없는 글은 서비스가 거른다
                        .requestMatchers(HttpMethod.GET, "/api/*/series/{seriesId:\\d+}", "/api/*/posts/{postId:\\d+}/series",
                                "/api/*/posts/{postId:\\d+}/related").permitAll()
                        // 공지사항 읽기는 누구나. 쓰기는 로그인한 관리자만(서비스가 역할을 확인, specs/012 D-2)
                        .requestMatchers(HttpMethod.GET, "/api/*/notices", "/api/*/notices/**").permitAll()
                        // 글마당 읽기는 누구나 (specs/017 US1)
                        .requestMatchers(HttpMethod.GET, "/api/*/board/**").permitAll()
                        // 이미지 보기 (005 contracts 11). 볼 수 있는지는 서비스가 글·올린 사람을 보고 정한다. 본문에 저장되는 주소라 버전 없이 고정
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        // 로봇이 읽는 주소: 링크 미리보기 카드, RSS, 사이트 지도, 검색엔진 안내 (specs/007)
                        .requestMatchers(HttpMethod.GET, "/og", "/og/", "/og/blog/{blogId:\\d+}", "/og/posts/{postId:\\d+}")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/rss.xml", "/blog/{blogId:\\d+}/rss.xml", "/sitemap.xml",
                                "/robots.txt").permitAll()
                        // 원고 편지 그만 받기 (016 contracts 3). 메일의 서명된 값으로 로그인 없이
                        .requestMatchers(HttpMethod.POST, "/api/*/newsletter/unsubscribe").permitAll()
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
