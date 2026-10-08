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
        User saved = users.saveAndFlush(User.create("  ChulSoo@Example.COM ", "hash", "철수"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("chulsoo@example.com");
        assertThat(saved.getFailedLoginCount()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull(); // JPA Auditing이 채운다
        assertThat(saved.isLocked(Instant.now())).isFalse();
    }

    @Test
    void 이메일과_닉네임은_대소문자를_무시하고_찾는다() {
        users.saveAndFlush(User.create("younghee@example.com", "hash", "YoungHee"));

        assertThat(users.findActiveByEmail("YoungHee@Example.com")).isPresent();
        assertThat(users.existsActiveByEmail("YOUNGHEE@example.com")).isTrue();
        assertThat(users.existsActiveByNickname("younghee")).isTrue();
        assertThat(users.existsActiveByNickname("someone")).isFalse();
    }

    @Test
    void 조회할_때도_앞뒤_공백을_지우고_찾는다() {
        users.saveAndFlush(User.create("mina@example.com", "hash", " Mina "));

        assertThat(users.existsActiveByNickname(" mina ")).isTrue();
        assertThat(users.findActiveByEmail("  MINA@example.com ")).isPresent();
    }

    @Test
    void 같은_이메일은_대소문자가_달라도_두_번_저장할_수_없다() {
        users.saveAndFlush(User.create("dup@example.com", "hash", "중복1"));

        assertThatThrownBy(() -> users.saveAndFlush(User.create("DUP@example.com", "hash", "중복2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_닉네임은_대소문자가_달라도_두_번_저장할_수_없다() {
        users.saveAndFlush(User.create("nick1@example.com", "hash", "Minsu"));

        assertThatThrownBy(() -> users.saveAndFlush(User.create("nick2@example.com", "hash", "MINSU")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 두_회원이_대소문자만_다른_같은_닉네임으로_바꾸면_DB가_두_번째를_막는다() {
        // specs/002 T011, quickstart S-2의 10, SC-002: 미리 확인을 지나쳐도 DB의 중복 불가가 마지막으로 막는다
        User first = users.saveAndFlush(User.create("change1@example.com", "hash", "바꾸기1"));
        User second = users.saveAndFlush(User.create("change2@example.com", "hash", "바꾸기2"));

        first.changeProfile("NewName", null);
        users.saveAndFlush(first);
        second.changeProfile("newname", null);

        assertThatThrownBy(() -> users.saveAndFlush(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 나를_빼고_닉네임_중복을_확인한다() {
        User me = users.saveAndFlush(User.create("except1@example.com", "hash", "나야나"));
        users.saveAndFlush(User.create("except2@example.com", "hash", "너야너"));

        assertThat(users.existsActiveByNicknameExcept("나야나", me.getId())).isFalse();
        assertThat(users.existsActiveByNicknameExcept(" 너야너 ", me.getId())).isTrue();
        assertThat(users.findActiveById(me.getId())).isPresent();
    }
}
