package com.myblog.ai.service;

import java.util.List;

/**
 * 본문을 받아 고칠 곳을 제안하는 교정기 (specs/014 D-1, D-15). Claude, 규칙 교정, 꺼짐 가운데 설정에 따라 하나가 빈으로 뜬다.
 * 결과는 믿지 않는다: 위치는 {@link ProofreadChecker}가 본문에서 다시 찾는다.
 */
public interface Proofreader {

    /** 지금 교정할 수 있나 (열쇠가 없으면 거짓, 화면은 단추를 숨긴다). */
    boolean available();

    /**
     * 어떤 교정기인가: {@code ai}(밖의 AI에 글을 보낸다, 하루 횟수를 센다), {@code rules}(서버 안의 규칙, 횟수를 세지 않는다),
     * {@code off}. 화면은 ai일 때만 보내기 동의를 묻는다.
     */
    String mode();

    /** 고칠 곳 제안. 늦거나 실패하거나 거절하면 {@link UnavailableException}. */
    List<Suggestion> proofread(String content);

    /**
     * 고칠 곳 하나. kind는 spacing·spelling·style, context는 원문 바로 앞 글자(본문에서 위치를 찾는 데 쓴다).
     */
    record Suggestion(String kind, String context, String original, String replacement, String reason) {
    }

    class UnavailableException extends RuntimeException {

        public UnavailableException(String message) {
            super(message);
        }

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
