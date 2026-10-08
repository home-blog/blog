package com.myblog.user.controller.dto;

import com.myblog.user.validation.PasswordConfirmation;
import com.myblog.user.validation.PasswordConfirmed;
import com.myblog.user.validation.ValidIntro;
import com.myblog.user.validation.ValidNickname;
import com.myblog.user.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/** 계정 관리 요청 본문 (specs/002 contracts). 회원 번호 칸은 두지 않는다: 회원은 세션으로만 정한다 (FR-002). */
public final class AccountRequests {

    private AccountRequests() {
    }

    /** 내 정보 저장. 이메일 칸이 없어서 요청에 이메일이 들어 있어도 무시된다 (FR-005). */
    public record UpdateProfile(@ValidNickname String nickname, @ValidIntro String intro) {
    }

    /**
     * 비밀번호 변경 (contracts 3). 형식 검사는 비밀번호를 비교하기 전에 하므로, 형식이 틀린 요청은 실패 횟수에 넣지 않는다.
     * 확인 일치 검사는 가입과 같은 @PasswordConfirmed를 newPasswordConfirm 칸에 쓴다 (FR-017).
     */
    @PasswordConfirmed(confirmField = "newPasswordConfirm")
    public record ChangePassword(
            @NotBlank(message = "CURRENT_PASSWORD_REQUIRED") String currentPassword,
            @ValidPassword String newPassword,
            String newPasswordConfirm) implements PasswordConfirmation {

        @Override
        public String password() {
            return newPassword;
        }

        @Override
        public String passwordConfirm() {
            return newPasswordConfirm;
        }
    }

    /**
     * 회원 탈퇴 (contracts 4). agreed는 boolean이다: Boolean이면 비어 있을 때 @AssertTrue를 통과한다 (FR-025).
     */
    public record Withdraw(
            @NotBlank(message = "PASSWORD_REQUIRED") String password,
            @AssertTrue(message = "WITHDRAWAL_NOT_AGREED") boolean agreed) {
    }
}
