package com.myblog.user.social;

import java.io.Serializable;
import java.time.Instant;

/**
 * 회사 화면을 오가는 동안 세션에 잠깐 두는 값 (specs/010). 세션은 DB에 직렬화되어 저장된다.
 * 둘 다 social.pending-ttl(10분) 뒤에는 없는 것으로 본다 (D-8).
 */
public final class SocialSessionState {

    /** 처음 들어온 사람의 가입 대기 (US2). */
    public static final String PENDING = "myblog.social.pending";
    /** 로그인한 회원이 "연결하기"를 눌렀다 (US4). */
    public static final String LINK = "myblog.social.link";

    private SocialSessionState() {
    }

    /** 가입 대기. nickname은 회사가 알려 준 이름 중 규칙에 맞고 쓰이지 않은 것(제안), 없으면 null. */
    public record Pending(SocialProvider provider, String providerUserId, String email, boolean emailVerified,
            String nickname, Instant expiresAt) implements Serializable {

        public boolean expired(Instant now) {
            return !now.isBefore(expiresAt);
        }

        /** 회사가 확인해 준 이메일이 있어서 이메일 인증을 건너뛰는가. */
        public boolean hasVerifiedEmail() {
            return emailVerified && email != null;
        }
    }

    /** 연결하기: 이 회원에 이 회사 계정을 연결하려 한다. */
    public record LinkIntent(Long memberId, SocialProvider provider, Instant expiresAt) implements Serializable {

        public boolean expired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
