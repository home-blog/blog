package com.myblog.user.service;

import com.myblog.user.config.AuthProperties;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 연속 실패와 잠금 (specs/001 US3, FR-027 ~ FR-029).
 * 같은 계정으로 5번 연속 틀리면 10분 동안 잠근다. 잠금 시각이 지나면 다음 시도 때 횟수를 0으로 돌린다 (따로 도는 작업 없음).
 */
@Service
public class LoginAttemptService {

    private final UserRepository users;
    private final AuthProperties properties;
    private final Clock clock;

    public LoginAttemptService(UserRepository users, AuthProperties properties, Clock clock) {
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    /** 비밀번호가 틀렸다. 가입되지 않은 이메일은 기록하지 않는다 (US3-4). */
    @Transactional
    public void recordFailure(String email) {
        Optional<User> found = users.findActiveByEmail(User.normalizeEmail(email));
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        Instant now = Instant.now(clock);
        if (user.getLockedUntil() != null && !user.isLocked(now)) {
            users.resetLoginFailures(user.getId()); // 잠금이 풀린 뒤 처음 틀림 → 처음부터 다시 센다
        }
        users.incrementFailedLoginCount(user.getId());
        if (users.findFailedLoginCount(user.getId()) >= properties.login().maxFailedAttempts()) {
            users.lockUntil(user.getId(), now.plus(properties.login().lockDuration()));
        }
    }

    /** 로그인 성공: 횟수를 0으로 되돌린다. */
    @Transactional
    public void recordSuccess(Long memberId) {
        users.resetLoginFailures(memberId);
    }

    /** 잠겨 있다면 남은 시간(초, 올림). 잠겨 있지 않으면 비어 있다. */
    @Transactional(readOnly = true)
    public Optional<Long> lockedForSeconds(String email) {
        Instant now = Instant.now(clock);
        return users.findActiveByEmail(User.normalizeEmail(email))
                .filter(user -> user.isLocked(now))
                .map(user -> {
                    long millis = user.getLockedUntil().toEpochMilli() - now.toEpochMilli();
                    return (millis + 999) / 1000;
                });
    }
}
