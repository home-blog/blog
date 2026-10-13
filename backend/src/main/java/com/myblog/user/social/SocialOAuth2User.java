package com.myblog.user.social;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 회사 로그인을 막 마친 사람. 성공 처리기(SocialLoginSuccessHandler)가 곧바로 우리 회원(MemberPrincipal)으로 바꾸거나
 * 로그인 정보를 지우므로 세션에 남지 않는다. 권한이 없어서 이 상태로는 로그인이 필요한 API를 부를 수 없다.
 */
public record SocialOAuth2User(SocialProfile profile) implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("provider", profile.provider().id(), "id", profile.providerUserId());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return profile.provider().id() + ":" + profile.providerUserId();
    }
}
