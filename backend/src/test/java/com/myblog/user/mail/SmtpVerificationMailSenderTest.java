package com.myblog.user.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

/** 메일 발송 실패 로그에는 예외 종류만 남고, 받는 사람 주소나 메일 서버 응답이 든 예외 메시지는 남지 않는다. */
class SmtpVerificationMailSenderTest {

    @Test
    void 로그인_실패는_가장_안쪽_원인_종류까지_남긴다() {
        MailAuthenticationException e = new MailAuthenticationException(new AuthenticationFailedException("535 비밀"));
        assertThat(SmtpVerificationMailSender.causeTypes(e))
                .isEqualTo("MailAuthenticationException ← AuthenticationFailedException")
                .doesNotContain("535");
    }

    @Test
    void 메일별_실패에_든_받는_사람_주소는_남기지_않는다() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("someone@example.com");
        message.setText("인증번호: 123456");
        MailSendException e = new MailSendException(
                Map.of(message, new SendFailedException("550 5.1.1 <someone@example.com> 없는 주소")));
        assertThat(e.getMessage()).contains("someone@example.com");   // 원래 메시지에는 받는 사람이 들어 있다
        assertThat(SmtpVerificationMailSender.causeTypes(e))
                .isEqualTo("MailSendException ← SendFailedException")
                .doesNotContain("someone@example.com", "123456");
    }
}
