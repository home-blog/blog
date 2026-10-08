package com.myblog.user.controller.dto;

import com.myblog.user.validation.ValidIntro;
import com.myblog.user.validation.ValidNickname;

/** 계정 관리 요청 본문 (specs/002 contracts). 회원 번호 칸은 두지 않는다: 회원은 세션으로만 정한다 (FR-002). */
public final class AccountRequests {

    private AccountRequests() {
    }

    /** 내 정보 저장. 이메일 칸이 없어서 요청에 이메일이 들어 있어도 무시된다 (FR-005). */
    public record UpdateProfile(@ValidNickname String nickname, @ValidIntro String intro) {
    }
}
