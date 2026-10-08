package com.myblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.common.error.ApiException;
import com.myblog.user.domain.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 탈퇴와 비밀번호 변경이 같은 옛 비밀번호로 동시에 오면 둘 다 성공하지 않는다 (PR #19 리뷰).
 * 변경이 먼저면 탈퇴는 옛 비밀번호라 거절되고, 탈퇴가 먼저면 변경은 회원이 없어 거절된다.
 */
@SpringBootTest
class WithdrawalRaceTest {

    private static final String PASSWORD = "abcd123!";

    @Autowired
    private WithdrawalService withdrawal;

    @Autowired
    private PasswordChangeService passwordChange;

    @Autowired
    private MemberRegistration registration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private User member;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from category where blog_id in (select blog_id from blog where users_id = ?)", member.getId());
        jdbc.update("delete from blog where users_id = ?", member.getId());
        jdbc.update("delete from users where users_id = ?", member.getId());
    }

    @Test
    void 탈퇴와_비밀번호_변경이_동시에_와도_둘_다_성공하지_않는다() throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        member = registration.register("x" + id + "@example.com", passwordEncoder.encode(PASSWORD), "x" + id);
        List<String> succeeded = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> {
            start.await();
            try {
                passwordChange.change(member, PASSWORD, "changed12!", "no-session");
                succeeded.add("change");
            } catch (ApiException e) {
                // 탈퇴가 먼저 끝났다
            }
            return null;
        });
        pool.submit(() -> {
            start.await();
            try {
                withdrawal.withdraw(member, PASSWORD);
                succeeded.add("withdraw");
            } catch (ApiException e) {
                // 비밀번호가 먼저 바뀌었다
            }
            return null;
        });
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded).hasSize(1);
        boolean withdrawn = jdbc.queryForObject("select deleted_at is not null from users where users_id = ?",
                Boolean.class, member.getId());
        assertThat(withdrawn).isEqualTo(succeeded.contains("withdraw"));
    }
}
