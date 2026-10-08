package com.myblog.user.service;

import com.myblog.user.ActiveMemberLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ActiveMemberLock을 회원 표의 FOR SHARE 잠금으로 채운다. 같은 회원의 요청끼리는 서로 막지 않고,
 * 탈퇴(FOR UPDATE)와만 차례를 지킨다.
 */
@Component
public class ActiveMemberLockAdapter implements ActiveMemberLock {

    private static final String LOCK_SQL = "select count(*) from (select 1 from users"
            + " where users_id = ? and deleted_at is null for share) locked";

    private final JdbcTemplate jdbc;

    public ActiveMemberLockAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockIfActive(Long memberId) {
        Long count = jdbc.queryForObject(LOCK_SQL, Long.class, memberId);
        return count != null && count > 0;
    }
}
