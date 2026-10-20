package com.myblog.user.config;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import com.myblog.user.security.AccessKeyAuthenticationFilter;
import com.myblog.user.service.AccessKeyService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * 글결 MCP 주소(/api/{버전}/mcp)만의 보안 사슬 (specs/020 D-2, S1-3). 세션 사슬(SecurityConfig)보다 먼저 본다.
 * <ul>
 *   <li>개인 열쇠(Bearer)로만 로그인한다. 세션 쿠키는 읽지도 만들지도 않는다(그래서 CSRF가 필요 없다).</li>
 *   <li>열쇠가 없거나 틀리면 401과 {@code WWW-Authenticate: Bearer realm="geulgyeol"}.</li>
 *   <li>열쇠는 이 사슬에서만 읽는다: 다른 API에 열쇠를 보내도 로그인으로 보지 않는다.</li>
 * </ul>
 */
@Configuration
public class McpSecurityConfig {

    public static final String MCP_PATH = "/api/*/mcp";

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, AccessKeyService accessKeys, Clock clock)
            throws Exception {
        http
                .securityMatcher(MCP_PATH)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .addFilterAfter(new AccessKeyAuthenticationFilter(accessKeys, clock), SecurityContextHolderFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer realm=\"geulgyeol\"");
                            JsonErrorWriter.write(response, ErrorCode.UNAUTHENTICATED);
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                JsonErrorWriter.write(response, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        return http.build();
    }
}
