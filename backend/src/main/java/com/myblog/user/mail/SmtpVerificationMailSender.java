package com.myblog.user.mail;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 배포용: SMTP로 인증번호 메일을 보내고 끝날 때까지 기다린다 (research D-2). 계정은 환경 변수로만 넣는다 (D-1 미정).
 */
@Component
@Profile("!dev")
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
            throw new MailSendFailedException("인증번호 메일을 보내지 못했습니다", e);
        }
    }
}
