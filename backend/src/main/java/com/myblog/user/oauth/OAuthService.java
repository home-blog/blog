package com.myblog.user.oauth;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.AccessKeyGrant;
import com.myblog.user.oauth.OAuthPending.IssuedCode;
import com.myblog.user.oauth.OAuthPending.PendingRequest;
import com.myblog.user.oauth.OAuthStore.Client;
import com.myblog.user.oauth.OAuthStore.Grant;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.service.AccessKeyService.Verified;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글결 MCP의 작은 OAuth 2.1 인가 서버 (specs/023 D-1). 공개 클라이언트·인가 코드 + PKCE(S256)·갱신 토큰 돌리기만 한다.
 * <ul>
 *   <li>동적 등록(RFC 7591): 돌아갈 주소는 https 또는 http 루프백(localhost·127.0.0.1·[::1])만.</li>
 *   <li>인가: 모르는 앱·등록하지 않은 돌아갈 주소면 앱으로 돌려보내지 않고 글결 오류 화면으로 (RFC 6749 4.1.2.1).</li>
 *   <li>허락 코드는 한 번만. 같은 코드가 다시 오면 그 코드로 만든 연결을 끊는다 (RFC 6749 4.1.2, S1-4).</li>
 *   <li>갱신 토큰은 쓸 때마다 바뀌고, 옛 갱신 토큰이 다시 오면 연결을 끊는다 (OAuth 2.1 4.3.1, S1-5).</li>
 *   <li>출입증으로 MCP에 오면 연결의 범위(read·draft)를 개인 열쇠와 같은 {@link AccessKeyGrant}로 넘긴다.
 *       열쇠 번호와 겹치지 않게 연결 번호는 음수로 넣는다(요청 수 세기는 연결마다 따로).</li>
 * </ul>
 */
@Service
public class OAuthService {

    public static final String READ = AccessKeyGrant.READ;
    public static final String DRAFT = AccessKeyGrant.DRAFT;
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final Pattern CHALLENGE = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final int MAX_URI_LENGTH = 500;
    private static final int MAX_STATE_LENGTH = 500;
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);
    private static final String UNNAMED = "이름 없는 앱";

    private final OAuthStore store;
    private final OAuthPending pending;
    private final OAuthProperties properties;
    private final OAuthIssuer issuer;
    private final UserRepository users;
    private final Clock clock;

    public OAuthService(OAuthStore store, OAuthPending pending, OAuthProperties properties, OAuthIssuer issuer,
            UserRepository users, Clock clock) {
        this.store = store;
        this.pending = pending;
        this.properties = properties;
        this.issuer = issuer;
        this.users = users;
        this.clock = clock;
    }

    // ---- 동적 등록 (RFC 7591) ----

    /** 등록한 앱 (응답 그대로). */
    public record Registered(String clientId, Instant issuedAt, String clientName, List<String> redirectUris) {
    }

    @Transactional
    public Registered register(Map<String, Object> metadata, String ip) {
        if (pending.countRegistration(ip == null ? "unknown" : ip) > properties.registerPerHour()) {
            throw new OAuthError(HttpStatus.TOO_MANY_REQUESTS, "temporarily_unavailable",
                    "등록 요청이 너무 잦습니다. 잠시 뒤 다시 시도해 주세요");
        }
        Object rawUris = metadata.get("redirect_uris");
        if (!(rawUris instanceof List<?> list) || list.isEmpty() || list.size() > properties.maxRedirectUris()) {
            throw OAuthError.badRequest("invalid_redirect_uri",
                    "redirect_uris는 1~%d개여야 합니다".formatted(properties.maxRedirectUris()));
        }
        List<String> uris = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String uri) || !isAllowedRedirect(uri)) {
                throw OAuthError.badRequest("invalid_redirect_uri",
                        "돌아갈 주소는 https 또는 http://localhost·127.0.0.1만 됩니다(조각 # 없이)");
            }
            if (!uris.contains(uri)) {
                uris.add(uri);
            }
        }
        Object grantTypes = metadata.get("grant_types");
        if (grantTypes != null && (!(grantTypes instanceof List<?> types) || !types.contains("authorization_code"))) {
            throw OAuthError.badRequest("invalid_client_metadata", "grant_types에 authorization_code가 있어야 합니다");
        }
        Object responseTypes = metadata.get("response_types");
        if (responseTypes != null && (!(responseTypes instanceof List<?> types) || !types.contains("code"))) {
            throw OAuthError.badRequest("invalid_client_metadata", "response_types에 code가 있어야 합니다");
        }
        Object rawName = metadata.get("client_name");
        String name = rawName instanceof String text ? text.strip() : "";
        if (name.codePointCount(0, name.length()) > properties.clientNameMaxLength()) {
            throw OAuthError.badRequest("invalid_client_metadata",
                    "client_name은 %d자 이하여야 합니다".formatted(properties.clientNameMaxLength()));
        }
        if (name.isEmpty() || name.chars().anyMatch(Character::isISOControl)) {
            name = UNNAMED;
        }
        Instant now = Instant.now(clock);
        Client client = new Client(OAuthTokens.random(OAuthTokens.CLIENT_PREFIX, 24), name, List.copyOf(uris), now);
        store.insertClient(client);
        return new Registered(client.clientId(), now, name, client.redirectUris());
    }

    /** https(호스트 있음) 또는 http 루프백. 조각(#)·사용자 정보 없이. */
    static boolean isAllowedRedirect(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_URI_LENGTH || value.contains("\n")) {
            return false;
        }
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute() || uri.getRawFragment() != null || uri.getRawUserInfo() != null
                    || uri.getHost() == null) {
                return false;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            return "https".equals(scheme) || "http".equals(scheme) && isLoopback(uri);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isLoopback(URI uri) {
        return uri.getHost() != null && LOOPBACK_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    // ---- 인가 (RFC 6749 4.1.1, PKCE RFC 7636, RFC 8707) ----

    /** 인가 요청을 받고 어디로 보낼지. */
    public record AuthorizeOutcome(String location) {
    }

    public AuthorizeOutcome authorize(Map<String, String> params) {
        String clientId = params.get("client_id");
        Optional<Client> found = clientId == null || clientId.length() > 64 ? Optional.empty() : store.client(clientId);
        if (found.isEmpty()) {
            return new AuthorizeOutcome("/connect/authorize?error=invalid_client");
        }
        Client client = found.get();
        String requested = params.get("redirect_uri");
        String redirectUri = requested == null ? (client.redirectUris().size() == 1 ? client.redirectUris().get(0) : null)
                : matchRedirect(client, requested);
        if (redirectUri == null) {
            return new AuthorizeOutcome("/connect/authorize?error=invalid_client");
        }
        String state = params.get("state");
        if (state != null && state.length() > MAX_STATE_LENGTH) {
            return redirectError(redirectUri, "invalid_request", null);
        }
        if (!"code".equals(params.get("response_type"))) {
            return redirectError(redirectUri, "unsupported_response_type", state);
        }
        String challenge = params.get("code_challenge");
        if (challenge == null || !CHALLENGE.matcher(challenge).matches()
                || !"S256".equals(params.get("code_challenge_method"))) {
            return redirectError(redirectUri, "invalid_request", state);
        }
        String scope;
        try {
            scope = requestedScope(params.get("scope"));
        } catch (OAuthError e) {
            return redirectError(redirectUri, e.error(), state);
        }
        String resource = params.get("resource");
        if (resource != null && !isOurResource(resource)) {
            return redirectError(redirectUri, "invalid_target", state);
        }
        try {
            String id = pending.saveRequest(new PendingRequest(client.clientId(), client.name(), redirectUri,
                    requested != null, state, challenge, scope, resource,
                    Instant.now(clock).plus(properties.requestTtl())));
            return new AuthorizeOutcome("/connect/authorize?request=" + id);
        } catch (OAuthError e) {
            return redirectError(redirectUri, e.error(), state);
        }
    }

    /**
     * 등록한 돌아갈 주소와 같은가. 루프백(http://localhost 등)은 포트만 달라도 된다 (RFC 8252 7.3).
     * 맞으면 요청에 온 주소를 그대로(포트 포함) 쓴다.
     */
    static String matchRedirect(Client client, String requested) {
        if (requested.length() > MAX_URI_LENGTH) {
            return null;
        }
        for (String registered : client.redirectUris()) {
            if (registered.equals(requested)) {
                return requested;
            }
            try {
                URI a = new URI(registered);
                URI b = new URI(requested);
                if ("http".equalsIgnoreCase(a.getScheme()) && "http".equalsIgnoreCase(b.getScheme()) && isLoopback(a)
                        && isLoopback(b) && a.getHost().equalsIgnoreCase(b.getHost())
                        && String.valueOf(a.getRawPath()).equals(String.valueOf(b.getRawPath()))
                        && String.valueOf(a.getRawQuery()).equals(String.valueOf(b.getRawQuery()))
                        && b.getRawFragment() == null && b.getRawUserInfo() == null) {
                    return requested;
                }
            } catch (Exception e) {
                // 주소 모양이 아니면 맞지 않는다
            }
        }
        return null;
    }

    /** scope: 공백으로 나눈 read·draft. 없으면 null(위임장에서 고른다), draft가 있으면 draft. 모르는 낱말은 invalid_scope. */
    static String requestedScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        boolean draft = false;
        boolean read = false;
        for (String word : raw.strip().split("\\s+")) {
            switch (word) {
                case READ -> read = true;
                case DRAFT -> draft = true;
                default -> throw OAuthError.badRequest("invalid_scope", "알 수 없는 scope입니다");
            }
        }
        return draft ? DRAFT : read ? READ : null;
    }

    private boolean isOurResource(String resource) {
        String ours = issuer.resource();
        return ours.equals(resource) || (ours + "/").equals(resource);
    }

    private AuthorizeOutcome redirectError(String redirectUri, String error, String state) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("error", error);
        if (state != null) {
            query.put("state", state);
        }
        query.put("iss", issuer.current());
        return new AuthorizeOutcome(withQuery(redirectUri, query));
    }

    static String withQuery(String uri, Map<String, String> query) {
        StringBuilder sb = new StringBuilder(uri);
        char separator = uri.contains("?") ? '&' : '?';
        for (Map.Entry<String, String> entry : query.entrySet()) {
            sb.append(separator).append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            separator = '&';
        }
        return sb.toString();
    }

    // ---- 위임장 (로그인한 회원, 세션 + CSRF) ----

    /** 위임장에 보일 것. scope는 앱이 요청한 범위(없으면 null). */
    public record RequestView(String clientName, String redirectHost, String scope, Instant expiresAt) {
    }

    public RequestView describe(String requestId) {
        PendingRequest request = consent(() -> pending.request(requestId)).orElseThrow(OAuthService::requestNotFound);
        return new RequestView(request.clientName(), URI.create(request.redirectUri()).getHost(), request.scope(),
                request.expiresAt());
    }

    /** 허락: 요청을 꺼내 허락 코드를 만들고 앱으로 돌아갈 주소를 돌려준다. 앱이 요청한 범위보다 넓게는 못 준다. */
    public String approve(Long memberId, String requestId, String scope) {
        if (!READ.equals(scope) && !DRAFT.equals(scope)) {
            throw new ApiException(ErrorCode.OAUTH_SCOPE_INVALID);
        }
        PendingRequest peek = consent(() -> pending.request(requestId)).orElseThrow(OAuthService::requestNotFound);
        if (READ.equals(peek.scope()) && DRAFT.equals(scope)) {
            throw new ApiException(ErrorCode.OAUTH_SCOPE_INVALID);
        }
        PendingRequest request = consent(() -> pending.takeRequest(requestId))
                .orElseThrow(OAuthService::requestNotFound);
        String code = consent(() -> pending.saveCode(new IssuedCode(memberId, request.clientId(),
                request.redirectUri(), request.redirectUriProvided(), request.codeChallenge(), scope,
                request.resource())));
        Map<String, String> query = new LinkedHashMap<>();
        query.put("code", code);
        if (request.state() != null) {
            query.put("state", request.state());
        }
        query.put("iss", issuer.current());
        return withQuery(request.redirectUri(), query);
    }

    public String deny(String requestId) {
        PendingRequest request = consent(() -> pending.takeRequest(requestId))
                .orElseThrow(OAuthService::requestNotFound);
        return redirectError(request.redirectUri(), "access_denied", request.state()).location();
    }

    /** 위임장 API는 세션 API라 Redis가 멈추면 공통 오류(503 SERVICE_UNAVAILABLE)로 답한다. */
    private static <T> T consent(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (OAuthError e) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private static ApiException requestNotFound() {
        return new ApiException(ErrorCode.OAUTH_REQUEST_NOT_FOUND);
    }

    // ---- 토큰 (RFC 6749 4.1.3, 6) ----

    /** 토큰 응답 (그대로 JSON으로). */
    public record TokenResponse(String accessToken, long expiresIn, String refreshToken, String scope) {
    }

    @Transactional(noRollbackFor = OAuthError.class)
    public TokenResponse token(Map<String, String> form) {
        String grantType = form.get("grant_type");
        if (grantType == null) {
            throw OAuthError.badRequest("invalid_request", "grant_type이 없습니다");
        }
        return switch (grantType) {
            case "authorization_code" -> exchangeCode(form);
            case "refresh_token" -> refresh(form);
            default -> throw OAuthError.badRequest("unsupported_grant_type", "authorization_code·refresh_token만 됩니다");
        };
    }

    private TokenResponse exchangeCode(Map<String, String> form) {
        String code = required(form, "code");
        Client client = client(required(form, "client_id"));
        String verifier = required(form, "code_verifier");
        Optional<IssuedCode> taken = pending.takeCode(code);
        if (taken.isEmpty()) {
            // 이미 쓴 코드가 다시 왔다: 그 코드로 만든 연결을 끊는다 (S1-4)
            pending.usedCodeGrant(code).ifPresent(store::deleteGrant);
            throw OAuthError.invalidGrant("허락 코드가 없거나, 지났거나, 이미 썼습니다");
        }
        IssuedCode issued = taken.get();
        String redirectUri = form.get("redirect_uri");
        if (!issued.clientId().equals(client.clientId())
                || (issued.redirectUriProvided() || redirectUri != null) && !issued.redirectUri().equals(redirectUri)) {
            throw OAuthError.invalidGrant("허락 코드를 받은 앱·돌아갈 주소와 다릅니다");
        }
        if (!OAuthTokens.verifierMatches(verifier, issued.codeChallenge())) {
            throw OAuthError.invalidGrant("code_verifier가 맞지 않습니다");
        }
        String resource = form.get("resource");
        if (resource != null && (!isOurResource(resource)
                || issued.resource() != null && !issued.resource().equals(resource))) {
            throw OAuthError.badRequest("invalid_target", "resource는 글결 MCP 주소여야 합니다");
        }
        Instant now = Instant.now(clock);
        // 회원 줄을 잡고 연결 수를 센다: 같은 회원의 연결 만들기·모두 끊기(비밀번호 변경·정지)가 한 줄로 선다
        users.findActiveByIdForUpdate(issued.memberId());
        if (users.findLoggedInById(issued.memberId(), now).isEmpty()) {
            throw OAuthError.invalidGrant("회원이 로그인할 수 없는 상태입니다");
        }
        long over = store.countGrants(issued.memberId()) - properties.maxGrantsPerMember() + 1;
        store.deleteLeastRecentlyUsed(issued.memberId(), (int) Math.max(0, over));
        String refresh = OAuthTokens.random(OAuthTokens.REFRESH_PREFIX, 32);
        long grantId = store.insertGrant(issued.memberId(), client.clientId(), issued.scope(),
                OAuthTokens.sha256Hex(refresh), now.plus(properties.refreshTokenTtl()), now);
        try {
            pending.markCodeUsed(code, grantId);
        } catch (OAuthError e) {
            // 표시를 남기지 못하면 코드를 두 번 쓴 것을 알아채지 못할 뿐이다. 코드는 이미 지워져 다시 쓸 수 없다
        }
        return new TokenResponse(issueAccess(grantId, now), properties.accessTokenTtl().toSeconds(), refresh,
                issued.scope());
    }

    private TokenResponse refresh(Map<String, String> form) {
        String refresh = required(form, "refresh_token");
        Client client = client(required(form, "client_id"));
        String hash = OAuthTokens.sha256Hex(refresh);
        Instant now = Instant.now(clock);
        // 범위는 돌리기 전에 본다: 돌린 뒤에 거절하면 앱 손의 갱신 토큰이 옛것이 되어 연결이 끊긴다
        String scope = form.get("scope");
        if (scope != null && !scope.isBlank() && DRAFT.equals(requestedScope(scope))
                && store.grantByRefresh(hash).flatMap(store::grant).filter(g -> READ.equals(g.scope())).isPresent()) {
            throw OAuthError.badRequest("invalid_scope", "허락받은 범위보다 넓게 받을 수 없습니다");
        }
        String next = OAuthTokens.random(OAuthTokens.REFRESH_PREFIX, 32);
        Optional<Grant> rotated = store.rotateRefresh(hash, client.clientId(), OAuthTokens.sha256Hex(next),
                now.plus(properties.refreshTokenTtl()), now);
        if (rotated.isEmpty()) {
            // 옛 갱신 토큰이 다시 왔다면 도난으로 보고 연결을 끊는다 (S1-5)
            store.grantByPreviousRefresh(hash).ifPresent(store::deleteGrant);
            throw OAuthError.invalidGrant("갱신 토큰이 없거나, 지났거나, 이미 바뀌었습니다");
        }
        Grant grant = rotated.get();
        if (users.findLoggedInById(grant.memberId(), now).isEmpty()) {
            store.deleteGrant(grant.grantId());
            throw OAuthError.invalidGrant("회원이 로그인할 수 없는 상태입니다");
        }
        return new TokenResponse(issueAccess(grant.grantId(), now), properties.accessTokenTtl().toSeconds(), next,
                grant.scope());
    }

    private String issueAccess(long grantId, Instant now) {
        String access = OAuthTokens.random(OAuthTokens.ACCESS_PREFIX, 32);
        store.insertAccessToken(OAuthTokens.sha256Hex(access), grantId, now.plus(properties.accessTokenTtl()));
        return access;
    }

    private Client client(String clientId) {
        return (clientId.length() > 64 ? Optional.<Client>empty() : store.client(clientId))
                .orElseThrow(() -> new OAuthError(HttpStatus.UNAUTHORIZED, "invalid_client", "등록되지 않은 앱입니다"));
    }

    private static String required(Map<String, String> form, String name) {
        String value = form.get(name);
        if (value == null || value.isBlank()) {
            throw OAuthError.badRequest("invalid_request", name + "이(가) 없습니다");
        }
        return value;
    }

    // ---- 폐기 (RFC 7009) ----

    /** 갱신 토큰이면 연결을, 출입증이면 그 출입증만 끊는다. 모르는 토큰도 조용히 지나간다. */
    @Transactional
    public void revoke(String token, String clientId) {
        if (token == null || token.isBlank() || token.length() > 200) {
            return;
        }
        String hash = OAuthTokens.sha256Hex(token);
        if (token.startsWith(OAuthTokens.REFRESH_PREFIX)) {
            store.grantByRefresh(hash).flatMap(store::grant)
                    .filter(grant -> clientId == null || grant.clientId().equals(clientId))
                    .ifPresent(grant -> store.deleteGrant(grant.grantId()));
        } else if (token.startsWith(OAuthTokens.ACCESS_PREFIX)) {
            store.deleteAccessToken(hash);
        }
    }

    // ---- MCP에서 출입증 확인 ----

    /** 출입증 확인 (S1-1). 모양이 다르거나, 없거나, 지났거나, 회원이 로그인할 수 없으면 비어 있다. */
    @Transactional
    public Optional<Verified> verify(String token) {
        if (token == null || !token.startsWith(OAuthTokens.ACCESS_PREFIX) || token.length() > 200) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return store.findByAccessToken(OAuthTokens.sha256Hex(token))
                .filter(found -> found.expiresAt().isAfter(now))
                .flatMap(found -> users.findLoggedInById(found.memberId(), now).map(user -> {
                    store.touch(found.grantId(), now, now.minus(TOUCH_INTERVAL));
                    return new Verified(user, new AccessKeyGrant(-found.grantId(), found.scope()));
                }));
    }

    public static boolean isAccessToken(String token) {
        return token != null && token.startsWith(OAuthTokens.ACCESS_PREFIX);
    }

    // ---- 연결된 앱 (마이페이지) ----

    public record ConnectedApp(long grantId, String clientName, String scope, Instant createdAt, Instant lastUsedAt) {
    }

    public record ConnectedApps(List<ConnectedApp> items, int max) {
    }

    @Transactional(readOnly = true)
    public ConnectedApps connectedApps(Long memberId) {
        List<ConnectedApp> items = store.grantsOf(memberId).stream()
                .map(grant -> new ConnectedApp(grant.grantId(), grant.clientName(), grant.scope(), grant.createdAt(),
                        grant.lastUsedAt()))
                .toList();
        return new ConnectedApps(items, properties.maxGrantsPerMember());
    }

    @Transactional
    public void disconnect(Long memberId, long grantId) {
        if (store.deleteGrant(grantId, memberId) == 0) {
            throw new ApiException(ErrorCode.OAUTH_GRANT_NOT_FOUND);
        }
    }

    /** 비밀번호 변경·탈퇴·정지: 이 회원의 연결을 모두 끊는다 (D-5). 부르는 쪽이 회원 줄을 잡은 트랜잭션 안에서 부른다. */
    public void revokeAll(Long memberId) {
        store.deleteGrantsOf(memberId);
    }

    // ---- 정리 ----

    @Transactional
    public void cleanup() {
        Instant now = Instant.now(clock);
        store.deleteExpiredAccessTokens(now);
        store.deleteExpiredGrants(now);
        store.deleteUnusedClients(now.minus(Duration.ofDays(properties.unusedClientDays())));
    }
}
