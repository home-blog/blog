package com.myblog.user.mail;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP로 인증번호 메일을 보내고 끝날 때까지 기다린다 (research D-2). 계정은 환경 변수로만 넣는다 (D-1 미정).
 * myblog.mail.sender=smtp(배포 기본값)일 때 켜진다. 개발 중에도 backend/.env에 계정을 넣으면 실제 메일함으로 받아 볼 수 있다.
 */
@Component
@ConditionalOnProperty(name = "myblog.mail.sender", havingValue = "smtp", matchIfMissing = true)
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpVerificationMailSender(JavaMailSender mailSender, @Value("${myblog.mail.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, String code, Duration validFor) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(VerificationMailSender.body(code, validFor));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // 서버 로그에서 원인을 바로 알 수 있게 예외 종류만 붙인다 (예: AuthenticationFailedException = 로그인 실패,
            // MailConnectException·SocketTimeoutException = 메일 서버에 연결 못 함). 예외 메시지에는 받는 사람 주소나 메일 서버 응답이
            // 들어갈 수 있어서 넣지 않는다. 화면에는 MAIL_SEND_FAILED의 고정 문구만 간다
            throw new MailSendFailedException("인증번호 메일을 보내지 못했습니다 (" + causeTypes(e) + ")", e);
        }
    }

    /** "MailSendException ← AuthenticationFailedException"처럼 바깥 예외와 가장 안쪽 원인의 종류 이름 */
    static String causeTypes(MailException e) {
        Throwable cause = e;
        if (e instanceof MailSendException send && send.getMessageExceptions().length > 0) {
            cause = send.getMessageExceptions()[0];   // 메일별 실패는 cause가 아니라 여기에 담긴다
        }
        Throwable root = NestedExceptionUtils.getMostSpecificCause(cause);
        String outer = e.getClass().getSimpleName();
        return root == e ? outer : outer + " ← " + root.getClass().getSimpleName();
    }
}
