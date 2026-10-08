package com.myblog.user.verification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 인증 비밀키 설정: 배포에서는 꼭 있어야 하고, 개발에서는 비어 있어도 켜진다. */
class EmailVerificationStoreSecretTest {

    private static final String LONG_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void 배포에서_비밀키가_없거나_짧으면_켜지지_않는다() {
        assertThatThrownBy(() -> new EmailVerificationStore(null, "", true, 32)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailVerificationStore(null, "short-secret", true, 32)).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new EmailVerificationStore(null, LONG_SECRET, true, 32)).doesNotThrowAnyException();
    }

    @Test
    void 개발에서는_비밀키가_없어도_임시_키로_켜진다() {
        assertThatCode(() -> new EmailVerificationStore(null, "", false, 32)).doesNotThrowAnyException();
    }

    @Test
    void 최소_길이를_하한보다_낮게_정하면_켜지_않는다() {
        assertThatThrownBy(() -> new EmailVerificationStore(null, LONG_SECRET, true, 16)).isInstanceOf(IllegalStateException.class);
    }
}
