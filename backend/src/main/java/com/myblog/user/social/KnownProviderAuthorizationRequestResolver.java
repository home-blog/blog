package com.myblog.user.social;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * 회사 화면으로 보낼 요청을 만든다. 열쇠가 없거나 모르는 회사(/oauth2/authorization/kakao 등)면 기본 동작인 500 대신
 * 요청을 그냥 지나가게 해서 다른 주소처럼 401로 끝나게 한다 (specs/010 D-9).
 */
public class KnownProviderAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String BASE_URI = "/oauth2/authorization";

    private final ClientRegistrationRepository registrations;
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public KnownProviderAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this.registrations = registrations;
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String prefix = BASE_URI + "/";
        if (path.startsWith(prefix) && registrations.findByRegistrationId(path.substring(prefix.length())) == null) {
            return null;
        }
        return delegate.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        if (registrations.findByRegistrationId(clientRegistrationId) == null) {
            return null;
        }
        return delegate.resolve(request, clientRegistrationId);
    }
}
