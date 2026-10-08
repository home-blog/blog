package com.myblog.user.config;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;

/**
 * 보안 설정 (specs/001 T011, plan.md 헌법 IV).
 * - 로그인은 서버 세션(Spring Session JDBC). 로그인할 때 세션 ID를 새로 만든다 (FR-032).
 * - 변경 요청(POST 등)은 CSRF 토큰을 헤더(X-XSRF-TOKEN)에 실어야 한다. 토큰은 GET /api/auth/csrf로 받는다 (NF-11).
 * - 로그인이 필요한 요청을 로그인 없이 보내면 401 + UNAUTHENTICATED (contracts 9).
 * - 기본은 "로그인 필요". 로그인 없이 열어 둘 주소는 아래에 하나씩 적는다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticated())
                        .accessDeniedHandler(accessDenied()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** 비밀번호는 BCrypt 해시로만 저장한다 (FR-010). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static AuthenticationEntryPoint unauthenticated() {
        return (request, response, exception) -> JsonErrorWriter.write(response, ErrorCode.UNAUTHENTICATED);
    }

    private static AccessDeniedHandler accessDenied() {
        return (request, response, exception) -> JsonErrorWriter.write(response,
                exception instanceof CsrfException ? ErrorCode.CSRF_TOKEN_INVALID : ErrorCode.FORBIDDEN);
    }
}
