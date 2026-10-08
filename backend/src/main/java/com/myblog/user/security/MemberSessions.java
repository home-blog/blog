package com.myblog.user.security;

import java.util.Set;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * 한 회원의 로그인 세션을 찾아 끝낸다 (specs/002 T020, FR-020, FR-027, research R-2).
 * 세션의 이름표(principal)는 MemberPrincipal.getUsername() = users_id이고, Spring Session JDBC가 세션 표의
 * PRINCIPAL_NAME 칸에 저장해 둔다. 그래서 회원 번호로 "이 회원의 세션 목록"을 찾을 수 있다.
 * 세션 저장소는 자기 트랜잭션으로 지우므로, 부르는 쪽의 트랜잭션에 묶이지 않는다 (research R-1).
 */
@Component
public class MemberSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public MemberSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** 지금 세션(keepSessionId)만 남기고 이 회원의 다른 세션을 모두 끝낸다. 끝낸 개수를 돌려준다. */
    public int expireOthers(Long memberId, String keepSessionId) {
        int expired = 0;
        for (String id : sessionIdsOf(memberId)) {
            if (!id.equals(keepSessionId)) {
                sessions.deleteById(id);
                expired++;
            }
        }
        return expired;
    }

    /** 이 회원의 세션을 모두 끝낸다 (탈퇴). */
    public int expireAll(Long memberId) {
        Set<String> ids = sessionIdsOf(memberId);
        ids.forEach(sessions::deleteById);
        return ids.size();
    }

    private Set<String> sessionIdsOf(Long memberId) {
        return Set.copyOf(sessions.findByPrincipalName(String.valueOf(memberId)).keySet());
    }
}
