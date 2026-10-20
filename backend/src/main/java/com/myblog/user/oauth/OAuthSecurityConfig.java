package com.myblog.user.oauth;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * OAuth 끝점(/.well-known/*, /oauth/*)만의 보안 사슬 (specs/023 D-7). 세션을 읽지도 만들지도 않는다.
 * <ul>
 *   <li>누구나 부른다(앱은 아직 로그인 전이다). 회원 확인은 위임장 API(세션 사슬)가 한다.</li>
 *   <li>CSRF 없음: 쿠키를 쓰지 않으므로 다른 사이트가 회원 이름으로 보낼 것이 없다.</li>
 *   <li>CORS는 모든 출처, 쿠키 없이: 브라우저에서 도는 MCP 클라이언트도 메타데이터·등록·토큰을 부른다.</li>
 * </ul>
 * 소셜 로그인의 /oauth2/·/login/oauth2/와 겹치지 않는다.
 */
@Configuration
public class OAuthSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("*"));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Authorization", "MCP-Protocol-Version"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/.well-known/**", cors);
        source.registerCorsConfiguration("/oauth/register", cors);
        source.registerCorsConfiguration("/oauth/token", cors);
        source.registerCorsConfiguration("/oauth/revoke", cors);

        http
                .securityMatcher("/.well-known/**", "/oauth/**")
                .cors(c -> c.configurationSource(source))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
