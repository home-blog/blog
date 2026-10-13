package com.myblog.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.myblog.user.config.SocialProperties;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 회사마다 다른 회원 정보를 같은 모양으로 맞추고, "인증된 이메일"을 research D-2대로 판정한다 (specs/010 T003). */
class SocialUserServiceTest {

    private static final SocialClientRegistrations REGISTRATIONS = new SocialClientRegistrations(new SocialProperties(null, null,
            Map.of("github", new SocialProperties.Client("g", "gs"), "google", new SocialProperties.Client("o", "os"),
                    "kakao", new SocialProperties.Client("k", "ks"), "naver", new SocialProperties.Client("n", "ns"))));

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer github = MockRestServiceServer.bindTo(builder).build();

    @Test
    void GitHub는_대표이면서_확인된_이메일만_인증된_이메일이다() {
        github.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("""
                        [{"email":"old@example.com","primary":false,"verified":true},
                         {"email":"Me@Example.com","primary":true,"verified":true}]""", MediaType.APPLICATION_JSON));

        SocialProfile profile = load("github", Map.of("id", 12345, "login", "octocat", "email", "public@example.com"));

        assertThat(profile).isEqualTo(new SocialProfile(SocialProvider.GITHUB, "12345", "Me@Example.com", true, "octocat"));
    }

    @Test
    void GitHub_이메일_목록을_못_읽으면_공개_이메일을_인증_안_된_것으로_쓴다() {
        github.expect(requestTo("https://api.github.com/user/emails")).andRespond(withServerError());

        SocialProfile profile = load("github", Map.of("id", 7, "login", "octocat", "email", "public@example.com"));

        assertThat(profile.email()).isEqualTo("public@example.com");
        assertThat(profile.emailVerified()).isFalse();
    }

    @Test
    void Google은_email_verified를_따른다() {
        assertThat(load("google", Map.of("sub", "g-1", "email", "a@gmail.com", "email_verified", true, "name", "에이")))
                .isEqualTo(new SocialProfile(SocialProvider.GOOGLE, "g-1", "a@gmail.com", true, "에이"));
        assertThat(load("google", Map.of("sub", "g-2", "email", "b@gmail.com")).emailVerified()).isFalse();
    }

    @Test
    void Kakao는_유효하고_확인된_이메일만_인증된_이메일이다() {
        Map<String, Object> account = Map.of("email", "k@kakao.com", "is_email_valid", true, "is_email_verified", true,
                "profile", Map.of("nickname", "카카오"));
        assertThat(load("kakao", Map.of("id", 99L, "kakao_account", account)))
                .isEqualTo(new SocialProfile(SocialProvider.KAKAO, "99", "k@kakao.com", true, "카카오"));

        Map<String, Object> invalid = Map.of("email", "k@kakao.com", "is_email_valid", false, "is_email_verified", true);
        assertThat(load("kakao", Map.of("id", 98L, "kakao_account", invalid)).emailVerified()).isFalse();

        // 이메일 동의가 없으면 이메일도 없다. 이름은 properties에서
        SocialProfile noEmail = load("kakao", Map.of("id", 97L, "properties", Map.of("nickname", "별명")));
        assertThat(noEmail.email()).isNull();
        assertThat(noEmail.name()).isEqualTo("별명");
    }

    @Test
    void Naver는_response_안의_값을_쓰고_이메일은_인증_안_된_것으로_본다() {
        Map<String, Object> response = Map.of("id", "nv-1", "email", "n@naver.com", "nickname", "네이버");
        assertThat(load("naver", Map.of("resultcode", "00", "response", response)))
                .isEqualTo(new SocialProfile(SocialProvider.NAVER, "nv-1", "n@naver.com", false, "네이버"));
    }

    private SocialProfile load(String provider, Map<String, Object> attributes) {
        ClientRegistration registration = REGISTRATIONS.findByRegistrationId(provider);
        String nameAttribute = registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        SocialUserService service = new SocialUserService(
                request -> new DefaultOAuth2User(java.util.List.of(), attributes, nameAttribute), builder.build());
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(),
                Instant.now().plusSeconds(60));
        return ((SocialOAuth2User) service.loadUser(new OAuth2UserRequest(registration, token))).profile();
    }
}
