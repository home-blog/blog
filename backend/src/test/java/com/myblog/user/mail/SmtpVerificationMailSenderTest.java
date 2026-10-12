package com.myblog.user.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 인증번호 메일: 보내는 사람은 서비스 이름으로 보이고(받는 사람·제목·본문 포함),
 * 실패 로그에는 예외 종류만 남고 받는 사람 주소나 메일 서버 응답이 든 예외 메시지는 남지 않는다.
 */
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
    void 보내지_못하면_예외_종류만_붙인_MailSendFailedException() {
        doThrow(new MailAuthenticationException(new AuthenticationFailedException("535 비밀"))).when(mailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(() -> new SmtpVerificationMailSender(mailSender, "sender@example.com", "글결")
                .send("reader@example.com", "123456", Duration.ofMinutes(5)))
                .isInstanceOf(MailSendFailedException.class)
                .hasMessageContaining("MailAuthenticationException ← AuthenticationFailedException")
                .hasMessageNotContaining("535");
    }

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

    private MimeMessage captureSent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
