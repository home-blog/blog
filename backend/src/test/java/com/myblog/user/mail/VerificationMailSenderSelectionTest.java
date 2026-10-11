package com.myblog.user.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** 인증번호 발송기 고르기: myblog.mail.sender 값에 따라 둘 중 하나만 켜진다. 값이 없으면(배포) 실제 SMTP. */
class VerificationMailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JavaMailSender.class, JavaMailSenderImpl::new)
            .withUserConfiguration(LoggingVerificationMailSender.class, SmtpVerificationMailSender.class);

    @Test
    void log로_정하면_로그_발송기만_켜진다() {
        runner.withPropertyValues("myblog.mail.sender=log").run(context -> {
            assertThat(context).hasSingleBean(VerificationMailSender.class);
            assertThat(context).hasSingleBean(LoggingVerificationMailSender.class);
        });
    }

    @Test
    void smtp로_정하면_SMTP_발송기만_켜진다() {
        runner.withPropertyValues("myblog.mail.sender=smtp").run(context -> {
            assertThat(context).hasSingleBean(VerificationMailSender.class);
            assertThat(context).hasSingleBean(SmtpVerificationMailSender.class);
        });
    }

    @Test
    void 값이_없으면_SMTP_발송기가_켜진다() {
        runner.run(context -> assertThat(context).hasSingleBean(SmtpVerificationMailSender.class));
    }
}
