package com.myblog.user.mail;

import java.time.Duration;

/**
 * 인증번호 메일 보내기 (FR-013, FR-014). 메일이 나갈 때까지 기다리고, 실패하면 MailSendFailedException을 던진다
 * (research D-2: 실패를 바로 알린다).
 */
public interface VerificationMailSender {

    void send(String email, String code, Duration validFor);

    /** 메일 본문. 서비스 이름, 인증번호, 유효 시간, 본인이 요청하지 않았을 때의 안내를 넣는다 (research B-9). */
    static String body(String code, Duration validFor) {
        return """
                MyBlog 이메일 인증번호입니다.

                인증번호: %s

                %d분 안에 가입 화면에 입력해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(code, validFor.toMinutes());
    }

    String SUBJECT = "[MyBlog] 이메일 인증번호";
}
