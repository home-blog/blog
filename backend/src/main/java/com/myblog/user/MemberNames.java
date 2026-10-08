package com.myblog.user;

import java.util.Collection;
import java.util.Map;

/**
 * 회원 번호로 이름을 알려 주는 틀 (specs/005 T008, FR-003, FR-007). 댓글 작성자처럼 다른 모듈이 회원 이름을 보여 줄 때 쓴다.
 * 여러 번호를 한 번에 읽는다 (댓글마다 따로 읽지 않음).
 * 탈퇴한 회원(deleted_at 있음)은 withdrawn이 참이고 <b>번호와 닉네임을 비운다</b>: 화면은 "탈퇴한 사용자"로 보여 준다
 * (005 D-9, 002 D-1. 바뀐 닉네임 탈퇴한사용자{id}를 그대로 보여 주지 않는다).
 */
public interface MemberNames {

    /** 받은 번호 중 회원 줄이 있는 것만 들어 있다. */
    Map<Long, MemberName> namesOf(Collection<Long> memberIds);

    record MemberName(Long id, String nickname, boolean withdrawn) {

        public static MemberName withdrawnMember() {
            return new MemberName(null, null, true);
        }
    }
}
