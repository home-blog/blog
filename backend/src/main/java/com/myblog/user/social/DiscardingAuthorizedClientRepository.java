package com.myblog.user.social;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * 회사 토큰을 어디에도 저장하지 않는다 (specs/010 D-7). 회원 정보를 읽는 데 한 번 쓰고 버린다.
 * 기본값(메모리 저장)이면 로그인할 때마다 토큰이 서버 메모리에 쌓인다.
 */
public class DiscardingAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal,
            HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal,
            HttpServletRequest request, HttpServletResponse response) {
        // 저장하지 않는다
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request,
            HttpServletResponse response) {
        // 저장한 것이 없다
    }
}
