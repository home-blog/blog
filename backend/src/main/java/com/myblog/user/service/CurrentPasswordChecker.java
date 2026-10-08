package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * 현재 비밀번호 확인은 한곳에서 (specs/002 T024, research B-2, FR-014, FR-018, FR-022).
 * 로그인과 같은 AuthenticationManager를 그대로 부른다. 그러면 이미 있는 코드로 아래가 모두 된다.
 * - 잠겨 있으면 비밀번호를 비교하지 않고 거절 (MemberPrincipal.isAccountNonLocked → LockedException)
 * - 틀리면 로그인 실패와 같은 칸에 +1, 5번째에 잠금 (LoginAttemptListener → LoginAttemptService.recordFailure)
 * - 맞으면 횟수를 0으로 (recordSuccess, D-4)
 * 비밀번호 변경·탈퇴 트랜잭션보다 먼저, 그 밖에서 부른다. 안에서 부르면 뒤의 오류로 +1까지 되돌려진다.
 */
@Service
public class CurrentPasswordChecker {

    /** D-3 문구. 두 %d는 실패 횟수(설정값)와 남은 분(올림). */
    static final String LOCKED_MESSAGE = "비밀번호를 %d회 잘못 입력해 잠겼습니다. %d분 뒤에 다시 시도해 주세요";

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService attempts;

    public CurrentPasswordChecker(AuthenticationManager authenticationManager, LoginAttemptService attempts) {
        this.authenticationManager = authenticationManager;
        this.attempts = attempts;
    }

    /**
     * 맞으면 그대로 돌아온다. 잠겨 있거나 방금 잠겼으면 423 ACCOUNT_LOCKED,
     * 틀리면 400 CURRENT_PASSWORD_MISMATCH(currentPassword 칸). 401이 아니다: 401은 로그인 창을 띄우는 약속이다.
     */
    public void check(String email, String password, String field) {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (LockedException e) {
            throw attempts.lockedError(email, LOCKED_MESSAGE);
        } catch (BadCredentialsException e) {
            if (attempts.lockedForSeconds(email).isPresent()) {
                throw attempts.lockedError(email, LOCKED_MESSAGE); // 방금 5번째로 틀려 잠겼다
            }
            ErrorCode code = ErrorCode.CURRENT_PASSWORD_MISMATCH;
            throw new ApiException(code, code.message(), List.of(new ErrorResponse.FieldErrorItem(field, code.name(), code.message())));
        }
    }
}
