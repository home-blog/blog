package com.myblog.user.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** 인증번호 메일의 보내는 사람·받는 사람·제목·본문 (보내는 사람은 서비스 이름으로 보인다). */
class SmtpVerificationMailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void 보내는_사람은_서비스_이름과_주소() throws Exception {
        new SmtpVerificationMailSender(mailSender, "sender@example.com", "글결")
                .send("reader@example.com", "123456", Duration.ofMinutes(5));

        MimeMessage sent = captureSent();
        InternetAddress from = (InternetAddress) sent.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("sender@example.com");
        assertThat(from.getPersonal()).isEqualTo("글결");
        assertThat(sent.getAllRecipients()).extracting(Object::toString).containsExactly("reader@example.com");
        assertThat(sent.getSubject()).isEqualTo(VerificationMailSender.SUBJECT);
        assertThat((String) sent.getContent()).contains("123456").contains("5분");
    }

    @Test
    void 이름이_없으면_주소만() throws Exception {
        new SmtpVerificationMailSender(mailSender, "sender@example.com", "")
                .send("reader@example.com", "123456", Duration.ofMinutes(5));

        InternetAddress from = (InternetAddress) captureSent().getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("sender@example.com");
        assertThat(from.getPersonal()).isNull();
    }

    @Test
    void 보내지_못하면_MailSendFailedException() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> new SmtpVerificationMailSender(mailSender, "sender@example.com", "글결")
                .send("reader@example.com", "123456", Duration.ofMinutes(5)))
                .isInstanceOf(MailSendFailedException.class);
    }

    private MimeMessage captureSent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
