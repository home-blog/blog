package com.myblog.subscription.mail;

/** 원고 편지 한 통을 보낸다 (specs/016 US3). 실패하면 예외를 낸다(부르는 쪽이 기록만 남기고 넘어간다). */
public interface NewsletterMailer {

    void send(String to, String subject, String body);
}
