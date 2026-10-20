package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.MemberModeration;
import com.myblog.user.config.MemberModerationProperties;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.security.MemberSessions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MemberModeration을 회원 표로 채운다 (specs/022 US2, D-4 ~ D-6).
 * 정지는 회원 줄을 잠그고 적은 뒤 열쇠를 끊고(같은 줄을 잡는 열쇠 만들기와 차례로) 세션을 모두 끝낸다.
 * 세션을 끝내는 사이 새로 생긴 세션은 LoggedInMember가 정지를 보고 로그인하지 않은 것으로 다룬다 (S2-3).
 */
@Service
public class MemberModerationService implements MemberModeration {

    private final UserRepository users;
    private final AccessKeyService accessKeys;
    private final MemberSessions sessions;
    private final MemberModerationProperties properties;
    private final Clock clock;

    public MemberModerationService(UserRepository users, AccessKeyService accessKeys, MemberSessions sessions,
            MemberModerationProperties properties, Clock clock) {
        this.users = users;
        this.accessKeys = accessKeys;
        this.sessions = sessions;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public List<Integer> suspendDays() {
        return properties.suspendDays().stream().sorted().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count(String query) {
        return users.countActiveByNickname(pattern(query));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberRow> search(String query, long offset, int limit) {
        Instant now = Instant.now(clock);
        return users.searchActiveByNickname(pattern(query), PageRequest.of((int) (offset / limit), limit)).stream()
                .map(user -> {
                    boolean suspended = user.isSuspended(now);
                    return new MemberRow(user.getId(), user.getNickname(), user.getRole(), user.getCreatedAt(),
                            suspended ? user.getSuspendedUntil() : null, suspended ? user.getSuspendReason() : null);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long suspendedCount() {
        return users.countSuspended(Instant.now(clock));
    }

    @Override
    @Transactional
    public Suspension suspend(Long adminId, Long memberId, int days, String reason) {
        if (!properties.suspendDays().contains(days)) {
            throw new ApiException(ErrorCode.SUSPEND_DAYS_INVALID);
        }
        User user = users.findActiveByIdForUpdate(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
        if (user.isAdmin() || user.getId().equals(adminId)) {
            throw new ApiException(ErrorCode.MEMBER_NOT_SUSPENDABLE);
        }
        Instant until = Instant.now(clock).plus(Duration.ofDays(days));
        user.suspend(until, reason);
        users.flush();
        accessKeys.revokeAll(memberId);
        sessions.expireAll(memberId);
        return new Suspension(user.getId(), user.getNickname(), until);
    }

    @Override
    @Transactional
    public Unsuspension unsuspend(Long memberId) {
        User user = users.findActiveByIdForUpdate(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
        boolean was = user.unsuspend(Instant.now(clock));
        return new Unsuspension(user.getId(), user.getNickname(), was);
    }

    /** 닉네임 일부를 LIKE 무늬로: 앞뒤 공백을 지우고 %·_·!를 !로 막는다(글자 그대로 찾기). */
    static String pattern(String query) {
        String q = query == null ? "" : query.strip();
        return "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
