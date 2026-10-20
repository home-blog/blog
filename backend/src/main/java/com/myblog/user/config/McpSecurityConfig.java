package com.myblog.user.config;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.JsonErrorWriter;
import com.myblog.user.oauth.OAuthIssuer;
import com.myblog.user.oauth.OAuthService;
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
 *   <li>개인 열쇠(Bearer gk_…)나 OAuth 출입증(Bearer go_…, specs/023)으로만 로그인한다.
 *       세션 쿠키는 읽지도 만들지도 않는다(그래서 CSRF가 필요 없다).</li>
 *   <li>열쇠가 없거나 틀리면 401과 {@code WWW-Authenticate: Bearer realm="geulgyeol", resource_metadata="…"}.
 *       resource_metadata(RFC 9728)를 보고 Claude 앱 같은 MCP 클라이언트가 OAuth로 붙는 법을 찾는다.</li>
 *   <li>열쇠는 이 사슬에서만 읽는다: 다른 API에 열쇠를 보내도 로그인으로 보지 않는다.</li>
 * </ul>
 */
@Configuration
public class McpSecurityConfig {

    public static final String MCP_PATH = "/api/*/mcp";

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, AccessKeyService accessKeys,
            OAuthService oauth, OAuthIssuer issuer, Clock clock) throws Exception {
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
                .addFilterAfter(new AccessKeyAuthenticationFilter(accessKeys, oauth, clock), SecurityContextHolderFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer realm=\"geulgyeol\", resource_metadata=\""
                                    + issuer.resourceMetadata(request) + "\"");
                            JsonErrorWriter.write(response, ErrorCode.UNAUTHENTICATED);
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                JsonErrorWriter.write(response, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        return http.build();
    }
}
