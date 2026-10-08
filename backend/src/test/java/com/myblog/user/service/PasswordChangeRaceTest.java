package com.myblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.common.error.ApiException;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
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

/** 같은 현재 비밀번호로 두 기기에서 동시에 바꾸면 하나만 성공하고, 늦은 쪽은 앞 변경을 덮어쓰지 않는다 (PR #18 리뷰). */
@SpringBootTest
class PasswordChangeRaceTest {

    private static final String PASSWORD = "abcd123!";

    @Autowired
    private PasswordChangeService passwordChange;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private User member;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from users where users_id = ?", member.getId());
    }

    @Test
    void 동시에_바꾸면_하나만_성공한다() throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        member = users.saveAndFlush(User.create("r" + id + "@example.com", passwordEncoder.encode(PASSWORD), "r" + id));
        List<String> newPasswords = List.of("first12!", "second12!");
        List<String> succeeded = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (String next : newPasswords) {
            pool.submit(() -> {
                start.await();
                try {
                    passwordChange.change(member, PASSWORD, next, "no-session");
                    succeeded.add(next);
                } catch (ApiException e) {
                    // 늦은 쪽: 현재 비밀번호가 이미 바뀌었다
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded).hasSize(1);
        String hash = jdbc.queryForObject("select password from users where users_id = ?", String.class, member.getId());
        assertThat(passwordEncoder.matches(succeeded.get(0), hash)).isTrue();
    }
}
