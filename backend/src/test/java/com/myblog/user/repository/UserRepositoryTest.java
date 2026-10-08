package com.myblog.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myblog.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** 회원 표(V2__auth_tables.sql)와 조회 규칙 (specs/001 T007, T013, E-2). 실제 PostgreSQL에서 확인한다. */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository users;

    @Test
    void 이메일은_공백을_지우고_소문자로_저장한다() {
        User saved = users.saveAndFlush(User.create("  ChulSoo@Example.COM ", "hash", "철수", Instant.now()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("chulsoo@example.com");
        assertThat(saved.getFailedLoginCount()).isZero();
        assertThat(saved.isLocked(Instant.now())).isFalse();
    }

    @Test
    void 이메일과_닉네임은_대소문자를_무시하고_찾는다() {
        users.saveAndFlush(User.create("younghee@example.com", "hash", "YoungHee", Instant.now()));

        assertThat(users.findActiveByEmail("YoungHee@Example.com")).isPresent();
        assertThat(users.existsActiveByEmail("YOUNGHEE@example.com")).isTrue();
        assertThat(users.existsActiveByNickname("younghee")).isTrue();
        assertThat(users.existsActiveByNickname("someone")).isFalse();
    }

    @Test
    void 같은_이메일은_대소문자가_달라도_두_번_저장할_수_없다() {
        users.saveAndFlush(User.create("dup@example.com", "hash", "중복1", Instant.now()));

        assertThatThrownBy(() -> users.saveAndFlush(User.create("DUP@example.com", "hash", "중복2", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_닉네임은_대소문자가_달라도_두_번_저장할_수_없다() {
        users.saveAndFlush(User.create("nick1@example.com", "hash", "Minsu", Instant.now()));

        assertThatThrownBy(() -> users.saveAndFlush(User.create("nick2@example.com", "hash", "MINSU", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
