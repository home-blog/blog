package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberSessions;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 비밀번호 변경 (specs/002 US2, contracts 3, FR-013 ~ FR-020).
 * 순서: ① 현재 비밀번호 확인(트랜잭션 밖, 틀린 횟수를 센다) ② 같은 값 거절
 * ③ 회원 줄을 잠그고 다시 확인한 뒤 새 해시 저장(트랜잭션) ④ 다른 기기의 세션 끝내기.
 * 세션 저장소는 자기 트랜잭션으로 지워서 ③과 한 묶음이 되지 않는다 (research R-1). 그래서 ④를 마지막에 하고,
 * ④가 실패하면 오류로 답해 다시 하게 한다 (다시 바꾸면 다시 지운다).
 */
@Service
public class PasswordChangeService {

    private final UserRepository users;
    private final CurrentPasswordChecker currentPassword;
    private final PasswordEncoder passwordEncoder;
    private final MemberSessions memberSessions;
    private final TransactionTemplate transaction;

    public PasswordChangeService(UserRepository users, CurrentPasswordChecker currentPassword, PasswordEncoder passwordEncoder,
            MemberSessions memberSessions, TransactionTemplate transaction) {
        this.users = users;
        this.currentPassword = currentPassword;
        this.passwordEncoder = passwordEncoder;
        this.memberSessions = memberSessions;
        this.transaction = transaction;
    }

    /** keepSessionId: 지금 기기의 세션. 이 세션만 로그인이 유지된다 (FR-020). */
    public void change(User member, String current, String newPassword, String keepSessionId) {
        currentPassword.check(member.getEmail(), current, "currentPassword");
        if (newPassword.equals(current)) {
            ErrorCode code = ErrorCode.PASSWORD_SAME_AS_CURRENT;
            throw new ApiException(code, code.message(), List.of(new ErrorResponse.FieldErrorItem("newPassword", code.name(), code.message())));
        }
        String hash = passwordEncoder.encode(newPassword);
        transaction.executeWithoutResult(status -> {
            // 회원 줄을 잠그고 현재 비밀번호를 한 번 더 확인한다. 두 기기에서 거의 동시에 바꾸면
            // 둘 다 ①을 통과하지만, 늦은 쪽은 여기서 앞 변경을 보고 거절된다 (덮어쓰지 않는다)
            User locked = users.findActiveByIdForUpdate(member.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
            if (!passwordEncoder.matches(current, locked.getPasswordHash())) {
                throw mismatch();
            }
            locked.changePasswordHash(hash);
        });
        memberSessions.expireOthers(member.getId(), keepSessionId);
    }

    private static ApiException mismatch() {
        ErrorCode code = ErrorCode.CURRENT_PASSWORD_MISMATCH;
        return new ApiException(code, code.message(), List.of(new ErrorResponse.FieldErrorItem("currentPassword", code.name(), code.message())));
    }
}
