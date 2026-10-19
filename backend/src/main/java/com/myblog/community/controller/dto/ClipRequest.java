package com.myblog.community.controller.dto;

/** 문장 수집 요청 (specs/015 contracts 3). 검사는 서비스가 한다. */
public record ClipRequest(Long postId, String text) {
}
