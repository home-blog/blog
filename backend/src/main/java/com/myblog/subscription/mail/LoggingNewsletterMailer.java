package com.myblog.subscription.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 개발·시험용: 원고 편지를 보내지 않고 서버 로그에 제목만 남긴다 (myblog.mail.sender=log). 받는 주소는 남기지 않는다. */
@Component
@ConditionalOnProperty(name = "myblog.mail.sender", havingValue = "log")
public class LoggingNewsletterMailer implements NewsletterMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingNewsletterMailer.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("원고 편지(보내지 않음): {}", subject);
    }
}
