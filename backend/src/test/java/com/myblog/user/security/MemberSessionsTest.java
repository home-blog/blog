package com.myblog.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * research R-2 확인: 회원 번호(users_id)로 그 회원의 세션을 찾아 지울 수 있다 (specs/002 T020).
 * 세션 저장소는 자기 트랜잭션으로 저장하므로 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
class MemberSessionsTest {

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> repository;

    @Autowired
    private MemberSessions memberSessions;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        created.forEach(repository::deleteById);
    }

    @Test
    void 지금_세션만_남기고_그_회원의_다른_세션을_끝낸다() {
        long a = uniqueMemberId();
        long b = a + 1;
        String keep = save(a);
        save(a);
        save(a);
        String other = save(b);

        int expired = memberSessions.expireOthers(a, keep);

        assertThat(expired).isEqualTo(2);
        assertThat(repository.findByPrincipalName(String.valueOf(a))).containsOnlyKeys(keep);
        assertThat(repository.findByPrincipalName(String.valueOf(b))).containsOnlyKeys(other);
    }

    @Test
    void 탈퇴하면_그_회원의_세션을_모두_끝낸다() {
        long a = uniqueMemberId();
        save(a);
        save(a);

        assertThat(memberSessions.expireAll(a)).isEqualTo(2);
        assertThat(repository.findByPrincipalName(String.valueOf(a))).isEmpty();
    }

    /** 로그인할 때처럼 이름표(principal)를 붙여 세션을 저장한다. */
    private String save(long memberId) {
        String id = save(repository, memberId);
        created.add(id);
        return id;
    }

    private static <S extends Session> String save(FindByIndexNameSessionRepository<S> sessions, long memberId) {
        S session = sessions.createSession();
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, String.valueOf(memberId));
        sessions.save(session);
        return session.getId();
    }

    /** 다른 테스트의 세션과 겹치지 않는 큰 번호 */
    private static long uniqueMemberId() {
        return 9_000_000_000L + (UUID.randomUUID().getMostSignificantBits() & 0xFFFFFFL) * 2;
    }
}
