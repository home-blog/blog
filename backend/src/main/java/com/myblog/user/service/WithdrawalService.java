package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.MemberWithdrawnEvent;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberSessions;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회원 탈퇴 (specs/002 US3, contracts 4, FR-022 ~ FR-030).
 * ① 비밀번호 확인(트랜잭션 밖. 틀리면 아무것도 지우지 않는다, SC-003)
 * ② 한 트랜잭션: 회원 줄 잠금 → 잠근 줄로 비밀번호 다시 확인 → 개인정보 지우기 → MemberWithdrawnEvent(각 모듈이 자기 데이터를 지움)
 * ③ 트랜잭션이 끝난 뒤 이 회원의 세션을 모두 끝낸다 (FR-027)
 */
@Service
public class WithdrawalService {

    private final UserRepository users;
    private final CurrentPasswordChecker currentPassword;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final MemberSessions memberSessions;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public WithdrawalService(UserRepository users, CurrentPasswordChecker currentPassword, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events, MemberSessions memberSessions, TransactionTemplate transaction, Clock clock) {
        this.users = users;
        this.currentPassword = currentPassword;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.memberSessions = memberSessions;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void withdraw(User member, String password) {
        currentPassword.check(member.getEmail(), password, "password");
        try {
            transaction.executeWithoutResult(status -> {
                User locked = users.findActiveByIdForUpdate(member.getId())
                        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
                // 잠근 줄의 비밀번호로 한 번 더 확인한다. ①과 이 사이에 다른 기기에서 비밀번호를 바꿨다면
                // 옛 비밀번호로 탈퇴되지 않게 거절한다 (PasswordChangeService와 같은 방식, PR #19 리뷰)
                if (!passwordEncoder.matches(password, locked.getPasswordHash())) {
                    ErrorCode code = ErrorCode.CURRENT_PASSWORD_MISMATCH;
                    throw new ApiException(code, code.message(),
                            List.of(new ErrorResponse.FieldErrorItem("password", code.name(), code.message())));
                }
                // 회원 줄을 먼저 바꿔 저장한다. 듣는 쪽이 일괄 삭제로 영속성 컨텍스트를 비우면
                // 그 뒤에 바꾼 값은 저장되지 않기 때문이다. 한 트랜잭션이라 어느 쪽이 실패해도 전부 취소된다
                locked.withdraw(Instant.now(clock));
                users.flush();
                events.publishEvent(new MemberWithdrawnEvent(locked.getId()));
            });
        } catch (DataIntegrityViolationException e) {
            // 탈퇴하는 동안 다른 기기에서 글을 쓴 경우 등: 전부 취소하고 다시 하게 한다 (research R-3)
            throw new ApiException(ErrorCode.WITHDRAWAL_CONFLICT);
        }
        memberSessions.expireAll(member.getId());
    }
}
