package com.myblog.user.validation;

/** 비밀번호와 비밀번호 확인을 함께 받는 요청. @PasswordConfirmed와 같이 쓴다. */
public interface PasswordConfirmation {

    String password();

    String passwordConfirm();
}
