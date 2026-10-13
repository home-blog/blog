package com.myblog.user.social;

import java.util.Arrays;
import java.util.Optional;

/** 소셜 로그인 회사 (specs/010). id는 주소(/oauth2/authorization/{id})와 표(user_social_account.provider)에 쓰는 이름이다. */
public enum SocialProvider {
    GITHUB("github"),
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String id;

    SocialProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<SocialProvider> of(String id) {
        return Arrays.stream(values()).filter(provider -> provider.id.equals(id)).findFirst();
    }
}
