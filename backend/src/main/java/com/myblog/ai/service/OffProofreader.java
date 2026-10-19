package com.myblog.ai.service;

import java.util.List;

/** 열쇠가 없어 꺼진 교정기 (specs/014 S1-6). 화면은 단추를 숨기고, 그래도 요청이 오면 503. */
public class OffProofreader implements Proofreader {

    @Override
    public String mode() {
        return "off";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<Suggestion> proofread(String content) {
        throw new UnavailableException("빨간 펜이 꺼져 있습니다");
    }
}
