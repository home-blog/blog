package com.myblog.user.service;

import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 회원과 그에 딸린 것(블로그, 미분류 분류)을 한 묶음으로 저장한다. 하나라도 실패하면 모두 취소된다 (FR-011). */
@Component
public class MemberRegistration {

    private final UserRepository users;
    private final List<MemberCreatedHandler> handlers;
    private final Clock clock;

    public MemberRegistration(UserRepository users, List<MemberCreatedHandler> handlers, Clock clock) {
        this.users = users;
        this.handlers = handlers;
        this.clock = clock;
    }

    @Transactional
    public User register(String email, String passwordHash, String nickname) {
        User member = users.saveAndFlush(User.create(email, passwordHash, nickname, Instant.now(clock)));
        for (MemberCreatedHandler handler : handlers) {
            handler.onMemberCreated(member);
        }
        return member;
    }
}
