package com.myblog.user;

import java.time.Instant;
import java.util.List;

/**
 * 편집실이 회원을 찾고 정지하는 입구 (specs/022 D-6). 편집실 모듈이 부르고 회원 모듈이 자기 표에 쓴다.
 * 정지하면 그 회원의 로그인 세션을 모두 끝내고 AI 비서 열쇠도 끊는다 (D-5). 정지 중에는 로그인하지 못한다.
 */
public interface MemberModeration {

    /** 고를 수 있는 정지 기간(일), 짧은 것부터. */
    List<Integer> suspendDays();

    /** 닉네임에 query가 들어 있는 회원 수 (탈퇴 제외, 대소문자 무시). query가 비면 모든 회원. */
    long count(String query);

    /** 닉네임으로 찾기, 최근 가입 순. offset은 limit의 배수다(쪽 번호에서 온다). */
    List<MemberRow> search(String query, long offset, int limit);

    /** 지금 정지 중인 회원 수 (운영 현황). */
    long suspendedCount();

    /**
     * 정지한다. 다시 정지하면 기간을 새로 정한다.
     * 없는·탈퇴한 회원 404 MEMBER_NOT_FOUND, 관리자(자기 자신 포함) 409 MEMBER_NOT_SUSPENDABLE, 기간이 목록에 없으면 400.
     */
    Suspension suspend(Long adminId, Long memberId, int days, String reason);

    /** 정지를 푼다. 없는·탈퇴한 회원 404. 정지 중이 아니었으면 unsuspended가 거짓. */
    Unsuspension unsuspend(Long memberId);

    /** suspendedUntil·suspendReason은 지금 정지 중일 때만 있다. role은 member·admin. */
    record MemberRow(Long memberId, String nickname, String role, Instant joinedAt, Instant suspendedUntil,
            String suspendReason) {
    }

    record Suspension(Long memberId, String nickname, Instant suspendedUntil) {
    }

    record Unsuspension(Long memberId, String nickname, boolean unsuspended) {
    }
}
