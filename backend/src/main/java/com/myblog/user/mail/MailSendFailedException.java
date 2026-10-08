package com.myblog.user.mail;

/** 인증번호 메일을 보내지 못했다. */
public class MailSendFailedException extends RuntimeException {

    public MailSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
