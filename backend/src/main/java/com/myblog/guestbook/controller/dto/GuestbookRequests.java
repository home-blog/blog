package com.myblog.guestbook.controller.dto;

/** 방명록 요청 본문 (specs/021 contracts 2, 4). 쓴 사람·블로그는 본문에서 받지 않는다 (세션과 주소로 정한다). */
public final class GuestbookRequests {

    private GuestbookRequests() {
    }

    /**
     * 엽서 쓰기. 검사는 서비스가 블로그를 확인한 <b>뒤에</b> 한다 (없는 블로그면 형식 오류보다 먼저 404).
     *
     * @param stamp  pine · wave · moon · plum · crane
     * @param secret 비밀 엽서(쓴 사람·주인만 읽음). 빼면 거짓
     */
    public record Write(String body, String stamp, Boolean secret) {

        public boolean wantsSecret() {
            return Boolean.TRUE.equals(secret);
        }
    }

    public record Reply(String body) {
    }
}
