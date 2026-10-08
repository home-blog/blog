package com.myblog.user.mail;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발용: 메일 대신 서버 로그에 인증번호를 찍는다 (research B-9). dev 설정에서만 켜지고 배포에서는 쓰지 않는다.
 * myblog.mail.simulate-failure=true로 켜면 발송 실패를 흉내 낸다 (quickstart S-3-7).
 */
@Component
@Profile("dev")
public class LoggingVerificationMailSender implements VerificationMailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationMailSender.class);

    private final boolean simulateFailure;

    public LoggingVerificationMailSender(@Value("${myblog.mail.simulate-failure:false}") boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public void send(String email, String code, Duration validFor) {
        if (simulateFailure) {
            throw new MailSendFailedException("개발용 설정으로 메일 발송 실패를 흉내 냄", null);
        }
        log.info("[개발용 메일] 받는 사람={} 인증번호={} ({}분 유효)", email, code, validFor.toMinutes());
    }
}
