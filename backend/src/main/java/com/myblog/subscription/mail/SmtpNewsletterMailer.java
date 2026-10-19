package com.myblog.subscription.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 서비스 메일 계정(spring.mail.*)으로 원고 편지를 보낸다. 인증번호 메일과 같은 계정·보내는 사람 이름("글결")을 쓴다
 * (myblog.mail.sender=smtp, 배포 기본값).
 */
@Component
@ConditionalOnProperty(name = "myblog.mail.sender", havingValue = "smtp", matchIfMissing = true)
public class SmtpNewsletterMailer implements NewsletterMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public SmtpNewsletterMailer(JavaMailSender mailSender,
            @Value("${myblog.mail.from:${spring.mail.username:}}") String from,
            @Value("${myblog.mail.from-name:}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (!from.isBlank()) {
                if (fromName.isBlank()) {
                    helper.setFrom(from);
                } else {
                    helper.setFrom(from, fromName);
                }
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("원고 편지를 만들지 못했습니다 (" + e.getClass().getSimpleName() + ")", e);
        }
    }
}
