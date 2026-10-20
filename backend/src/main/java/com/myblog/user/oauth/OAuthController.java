package com.myblog.user.oauth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메타데이터·동적 등록·인가·토큰·폐기 (specs/023 contracts 1~4). 세션을 쓰지 않는 사슬(OAuthSecurityConfig)에서 돈다.
 * 오류는 RFC 6749 모양 {@code {"error", "error_description"}}.
 */
@Tag(name = "19. AI 앱 연결(OAuth)", description = "Claude 앱 커스텀 커넥터처럼 OAuth로 글결 MCP에 붙기 (specs/023)")
@RestController
public class OAuthController {

    private static final List<String> SCOPES = List.of(OAuthService.READ, OAuthService.DRAFT);

    private final OAuthService oauth;
    private final OAuthIssuer issuer;

    public OAuthController(OAuthService oauth, OAuthIssuer issuer) {
        this.oauth = oauth;
        this.issuer = issuer;
    }

    @Operation(summary = "보호 자원 메타데이터 (RFC 9728)", description = "글결 MCP 주소와 인가 서버")
    @GetMapping(path = {"/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/api/v1/mcp"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> protectedResource() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resource", issuer.resource());
        body.put("authorization_servers", List.of(issuer.current()));
        body.put("scopes_supported", SCOPES);
        body.put("bearer_methods_supported", List.of("header"));
        body.put("resource_name", "글결");
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(body);
    }

    @Operation(summary = "인가 서버 메타데이터 (RFC 8414)")
    @GetMapping(path = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> authorizationServer() {
        String base = issuer.current();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuer", base);
        body.put("authorization_endpoint", base + "/oauth/authorize");
        body.put("token_endpoint", base + "/oauth/token");
        body.put("registration_endpoint", base + "/oauth/register");
        body.put("revocation_endpoint", base + "/oauth/revoke");
        body.put("response_types_supported", List.of("code"));
        body.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        body.put("code_challenge_methods_supported", List.of("S256"));
        body.put("token_endpoint_auth_methods_supported", List.of("none"));
        body.put("revocation_endpoint_auth_methods_supported", List.of("none"));
        body.put("scopes_supported", SCOPES);
        body.put("authorization_response_iss_parameter_supported", true);
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(body);
    }

    @Operation(summary = "동적 등록 (RFC 7591)", description = "공개 클라이언트만. 돌아갈 주소는 https 또는 http 루프백")
    @PostMapping(path = "/oauth/register", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@RequestBody(required = false) Map<String, Object> metadata,
            HttpServletRequest request) {
        if (metadata == null) {
            throw OAuthError.badRequest("invalid_client_metadata", "등록 정보가 없습니다");
        }
        OAuthService.Registered registered = oauth.register(metadata, request.getRemoteAddr());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", registered.clientId());
        body.put("client_id_issued_at", registered.issuedAt().getEpochSecond());
        body.put("client_name", registered.clientName());
        body.put("redirect_uris", registered.redirectUris());
        body.put("grant_types", List.of("authorization_code", "refresh_token"));
        body.put("response_types", List.of("code"));
        body.put("token_endpoint_auth_method", "none");
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(body);
    }

    @Operation(summary = "인가 (RFC 6749 4.1.1 + PKCE S256)", description = "글결 위임장 화면(/connect/authorize)으로 보낸다")
    @GetMapping("/oauth/authorize")
    public ResponseEntity<Void> authorize(@RequestParam Map<String, String> params) {
        OAuthService.AuthorizeOutcome outcome = oauth.authorize(params);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(outcome.location()))
                .cacheControl(CacheControl.noStore()).build();
    }

    @Operation(summary = "토큰 (authorization_code·refresh_token)")
    @PostMapping(path = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(@RequestParam MultiValueMap<String, String> form,
            HttpServletRequest request) {
        rejectQuery(request);
        OAuthService.TokenResponse token = oauth.token(single(form));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", token.accessToken());
        body.put("token_type", "Bearer");
        body.put("expires_in", token.expiresIn());
        body.put("refresh_token", token.refreshToken());
        body.put("scope", token.scope());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    @Operation(summary = "폐기 (RFC 7009)", description = "갱신 토큰이면 연결을, 출입증이면 그 출입증을 끊는다. 늘 200")
    @PostMapping(path = "/oauth/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> revoke(@RequestParam MultiValueMap<String, String> form, HttpServletRequest request) {
        rejectQuery(request);
        Map<String, String> values = single(form);
        oauth.revoke(values.get("token"), values.get("client_id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }

    /**
     * 토큰·폐기는 본문(form)으로만 받는다: 주소 뒤(?…)에 실린 코드·토큰은 접근 기록에 남으므로 거절한다
     * (RFC 6749 3.2, OAuth 2.1 보안 권고).
     */
    private static void rejectQuery(HttpServletRequest request) {
        if (request.getQueryString() != null) {
            throw OAuthError.badRequest("invalid_request", "토큰·폐기 요청은 주소가 아니라 본문으로 보내야 합니다");
        }
    }

    /** 같은 이름이 두 번 오면 잘못된 요청이다 (RFC 6749 3.1, 3.2). */
    private static Map<String, String> single(MultiValueMap<String, String> form) {
        Map<String, String> values = new LinkedHashMap<>();
        form.forEach((name, list) -> {
            if (list.size() > 1) {
                throw OAuthError.badRequest("invalid_request", "같은 값이 두 번 왔습니다");
            }
            values.put(name, list.get(0));
        });
        return values;
    }

    @ExceptionHandler(OAuthError.class)
    public ResponseEntity<Map<String, String>> oauthError(OAuthError error) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.error());
        body.put("error_description", error.getMessage());
        return ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
