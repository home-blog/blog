package com.myblog.user;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * 개인 열쇠로 들어온 요청이 받은 권한 (specs/020 US1). 회원 모듈이 MCP 주소에서 열쇠를 확인하면 로그인 정보의
 * details에 이것을 넣는다. 위 모듈(connect)은 열쇠 번호(요청 수 세기)와 범위(임시 저장 가능?)를 여기서만 읽는다.
 * 회원 번호는 지금처럼 {@link LoggedInMember}로 얻는다.
 *
 * @param scope {@code read} 또는 {@code draft}
 */
public record AccessKeyGrant(Long keyId, String scope) {

    public static final String READ = "read";
    public static final String DRAFT = "draft";

    /** 임시 저장(save_draft)까지 되는 열쇠인가. */
    public boolean canSaveDrafts() {
        return DRAFT.equals(scope);
    }

    /** 열쇠로 들어온 요청이면 그 권한, 아니면(세션 로그인·로그인 안 함) 비어 있다. */
    public static Optional<AccessKeyGrant> of(Authentication authentication) {
        return authentication != null && authentication.getDetails() instanceof AccessKeyGrant grant
                ? Optional.of(grant) : Optional.empty();
    }
}
