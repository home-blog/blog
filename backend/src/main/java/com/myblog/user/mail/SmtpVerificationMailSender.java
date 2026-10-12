package com.myblog.user.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP로 인증번호 메일을 보내고 끝날 때까지 기다린다 (research D-2). 계정은 환경 변수로만 넣는다 (D-1 미정).
 * myblog.mail.sender=smtp(배포 기본값)일 때 켜진다. 개발 중에도 .env에 계정을 넣으면 실제 메일함으로 받아 볼 수 있다.
 * 받는 사람에게는 보내는 사람이 "글결 &lt;주소&gt;"처럼 서비스 이름으로 보인다 (myblog.mail.from-name).
 */
@Component
@ConditionalOnProperty(name = "myblog.mail.sender", havingValue = "smtp", matchIfMissing = true)
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public SmtpVerificationMailSender(JavaMailSender mailSender,
            @Value("${myblog.mail.from:${spring.mail.username:}}") String from,
            @Value("${myblog.mail.from-name:}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public void send(String email, String code, Duration validFor) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (!from.isBlank()) {
                // 한글 이름은 메일 머리글 규칙대로 인코딩된다
                if (fromName.isBlank()) {
                    helper.setFrom(from);
                } else {
                    helper.setFrom(from, fromName);
                }
            }
            helper.setTo(email);
            helper.setSubject(SUBJECT);
            helper.setText(VerificationMailSender.body(code, validFor));
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new MailSendFailedException("인증번호 메일을 보내지 못했습니다", e);
        }
    }
}
