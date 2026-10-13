package com.myblog.user.social;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 회사의 회원 정보를 읽어 SocialProfile로 맞춘다 (specs/010 T003).
 * 회원 정보 주소는 스프링 기본(DefaultOAuth2UserService)으로 읽고, 회사마다 다른 모양만 여기서 푼다.
 * "인증된 이메일" 판정은 research D-2를 따른다.
 */
@Component
public class SocialUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(SocialUserService.class);
    private static final String GITHUB_EMAILS = "https://api.github.com/user/emails";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final RestClient restClient;

    public SocialUserService() {
        this(new DefaultOAuth2UserService(), RestClient.builder().requestFactory(timeouts()).build());
    }

    SocialUserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate, RestClient restClient) {
        this.delegate = delegate;
        this.restClient = restClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        SocialProvider provider = SocialProvider.of(request.getClientRegistration().getRegistrationId())
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("unknown_provider")));
        Map<String, Object> attributes = delegate.loadUser(request).getAttributes();
        SocialProfile profile = switch (provider) {
            case GITHUB -> github(attributes, request.getAccessToken().getTokenValue());
            case GOOGLE -> google(attributes);
            case KAKAO -> kakao(attributes);
            case NAVER -> naver(attributes);
        };
        if (profile.providerUserId() == null || profile.providerUserId().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_user_id"));
        }
        return new SocialOAuth2User(profile);
    }

    /** GitHub: 이메일은 목록에서 대표(primary)이면서 확인된(verified) 것만 인증된 이메일로 본다. 이름 제안은 아이디(login). */
    private SocialProfile github(Map<String, Object> attributes, String accessToken) {
        String email = null;
        boolean verified = false;
        Optional<Map<String, Object>> primary = githubPrimaryEmail(accessToken);
        if (primary.isPresent()) {
            email = text(primary.get().get("email"));
            verified = Boolean.TRUE.equals(primary.get().get("verified"));
        } else {
            email = text(attributes.get("email"));   // 목록을 못 읽었으면 공개 이메일. 확인 여부를 모르므로 인증 안 됨
        }
        return new SocialProfile(SocialProvider.GITHUB, text(attributes.get("id")), email, verified,
                text(attributes.get("login")));
    }

    private Optional<Map<String, Object>> githubPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get().uri(GITHUB_EMAILS)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            return emails == null ? Optional.empty()
                    : emails.stream().filter(email -> Boolean.TRUE.equals(email.get("primary"))).findFirst();
        } catch (RestClientException e) {
            log.info("GitHub 이메일 목록을 읽지 못함: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Google: email_verified가 참이면 인증된 이메일. */
    private static SocialProfile google(Map<String, Object> attributes) {
        return new SocialProfile(SocialProvider.GOOGLE, text(attributes.get("sub")), text(attributes.get("email")),
                Boolean.TRUE.equals(attributes.get("email_verified")), text(attributes.get("name")));
    }

    /** Kakao: kakao_account의 is_email_valid와 is_email_verified가 둘 다 참이면 인증된 이메일. */
    private static SocialProfile kakao(Map<String, Object> attributes) {
        Map<String, Object> account = map(attributes.get("kakao_account"));
        Map<String, Object> profile = map(account.get("profile"));
        boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"));
        String name = text(profile.get("nickname"));
        if (name == null) {
            name = text(map(attributes.get("properties")).get("nickname"));
        }
        return new SocialProfile(SocialProvider.KAKAO, text(attributes.get("id")), text(account.get("email")), verified, name);
    }

    /** Naver: 값이 response 안에 있다. 이메일 확인 여부를 알려 주지 않으므로 인증 안 된 것으로 본다 (D-2). */
    private static SocialProfile naver(Map<String, Object> attributes) {
        Map<String, Object> response = map(attributes.get("response"));
        String name = text(response.get("nickname"));
        if (name == null) {
            name = text(response.get("name"));
        }
        return new SocialProfile(SocialProvider.NAVER, text(response.get("id")), text(response.get("email")), false, name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).strip();
        return text.isEmpty() ? null : text;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
