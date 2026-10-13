package com.myblog.user.social;

import java.io.Serializable;

/**
 * 회사가 알려 준 회원 정보를 네 회사 같은 모양으로 맞춘 것 (specs/010 T003).
 * emailVerified는 "회사가 이 이메일을 확인했다"고 분명히 알려 준 경우에만 참이다 (D-2). name은 닉네임 제안에만 쓴다.
 */
public record SocialProfile(SocialProvider provider, String providerUserId, String email, boolean emailVerified, String name)
        implements Serializable {
}
