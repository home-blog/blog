package com.myblog.user.social;

import com.myblog.user.config.SocialProperties;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

/**
 * 열쇠가 들어 있는 회사만 담은 회사 목록 (specs/010 T002, D-9).
 * 스프링 기본 설정(spring.security.oauth2.client.*)은 열쇠가 비어 있으면 서버가 켜지지 않아서 쓰지 않고 여기서 만든다.
 * 회원 정보는 네 회사 모두 "회원 정보 주소"로 읽는다 (Google도 openid 없이 profile·email만, research 구현 방식).
 */
@Component
public class SocialClientRegistrations implements ClientRegistrationRepository {

    /** 회사가 돌려보낼 주소. {baseUrl}은 요청이 들어온 사이트 주소(앞단 Nginx·Vite가 넘긴 Host)다. */
    static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";

    private final Map<SocialProvider, ClientRegistration> registrations = new EnumMap<>(SocialProvider.class);

    public SocialClientRegistrations(SocialProperties properties) {
        for (SocialProvider provider : SocialProvider.values()) {
            SocialProperties.Client client = properties.clients().get(provider.id());
            if (client != null && client.configured()) {
                registrations.put(provider, build(provider, client));
            }
        }
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return SocialProvider.of(registrationId).map(registrations::get).orElse(null);
    }

    /** 쓸 수 있는 회사 (정해진 순서: github, google, kakao, naver). */
    public List<SocialProvider> enabled() {
        return List.copyOf(registrations.keySet());
    }

    public boolean isEnabled(SocialProvider provider) {
        return registrations.containsKey(provider);
    }

    private static ClientRegistration build(SocialProvider provider, SocialProperties.Client client) {
        return switch (provider) {
            // 이메일은 회원 정보에 없을 수 있어(비공개) 이메일 목록을 한 번 더 읽는다 (SocialUserService)
            case GITHUB -> CommonOAuth2Provider.GITHUB.getBuilder(provider.id())
                    .clientId(client.clientId()).clientSecret(client.clientSecret())
                    .redirectUri(REDIRECT_URI)
                    .scope("read:user", "user:email")
                    .build();
            case GOOGLE -> CommonOAuth2Provider.GOOGLE.getBuilder(provider.id())
                    .clientId(client.clientId()).clientSecret(client.clientSecret())
                    .redirectUri(REDIRECT_URI)
                    .scope("profile", "email")
                    .build();
            // 동의 항목은 카카오 앱 설정을 따른다 (scope를 보내지 않음): 이메일은 비즈 앱에서만 받을 수 있어서다
            case KAKAO -> ClientRegistration.withRegistrationId(provider.id())
                    .clientId(client.clientId()).clientSecret(client.clientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(REDIRECT_URI)
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .tokenUri("https://kauth.kakao.com/oauth/token")
                    .userInfoUri("https://kapi.kakao.com/v2/user/me")
                    .userNameAttributeName("id")
                    .clientName("Kakao")
                    .build();
            case NAVER -> ClientRegistration.withRegistrationId(provider.id())
                    .clientId(client.clientId()).clientSecret(client.clientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(REDIRECT_URI)
                    .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                    .tokenUri("https://nid.naver.com/oauth2.0/token")
                    .userInfoUri("https://openapi.naver.com/v1/nid/me")
                    .userNameAttributeName("response")
                    .clientName("Naver")
                    .build();
        };
    }
}
