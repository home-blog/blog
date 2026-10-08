package com.myblog.user.controller.dto;

import com.myblog.user.validation.PasswordConfirmation;
import com.myblog.user.validation.PasswordConfirmed;
import com.myblog.user.validation.ValidEmail;
import com.myblog.user.validation.ValidNickname;
import com.myblog.user.validation.ValidPassword;

/** 회원 가입·로그인 요청 본문 (specs/001 contracts 2 ~ 5). */
public final class AuthRequests {

    private AuthRequests() {
    }

    /** 인증번호 받기. 형식 검사는 서비스가 한다 (오류 code가 INVALID_EMAIL, INVALID_NICKNAME으로 나가야 해서). */
    public record SendCode(String nickname, String email) {
    }

    /** verificationToken: 인증번호를 받을 때 응답으로 받은 증표 */
    public record ConfirmCode(String email, String code, String verificationToken) {
    }

    public record CancelVerification(String email, String verificationToken) {
    }

    /** 가입하기. 어긴 칸마다 VALIDATION_FAILED의 fieldErrors로 알린다 (FR-009). */
    @PasswordConfirmed
    public record Signup(
            @ValidNickname String nickname,
            @ValidEmail String email,
            @ValidPassword String password,
            String passwordConfirm,
            String verificationToken) implements PasswordConfirmation {
    }
}
