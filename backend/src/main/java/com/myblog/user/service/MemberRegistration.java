package com.myblog.user.service;

import com.myblog.user.MemberRegisteredEvent;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원을 저장하고 "가입했다" 이벤트를 낸다. 이벤트를 받은 모듈(blog)이 같은 트랜잭션 안에서 블로그와 미분류 분류를 만든다.
 * 하나라도 실패하면 모두 취소된다 (FR-011).
 */
@Component
public class MemberRegistration {

    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public MemberRegistration(UserRepository users, ApplicationEventPublisher events) {
        this.users = users;
        this.events = events;
    }

    @Transactional
    public User register(String email, String passwordHash, String nickname) {
        User member = users.saveAndFlush(User.create(email, passwordHash, nickname));
        events.publishEvent(new MemberRegisteredEvent(member.getId(), member.getNickname()));
        return member;
    }
}
