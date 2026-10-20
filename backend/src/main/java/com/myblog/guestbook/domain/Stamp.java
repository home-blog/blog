package com.myblog.guestbook.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** 엽서에 붙이는 우표 다섯 가지 (specs/021 research D-5). 그림은 화면의 SVG다. 이름은 V30의 CHECK와 같다. */
public enum Stamp {
    PINE, WAVE, MOON, PLUM, CRANE;

    /** 화면·표에 쓰는 이름 (소문자). */
    public String code() {
        return name().toLowerCase();
    }

    public static Optional<Stamp> from(String code) {
        return code == null ? Optional.empty()
                : Arrays.stream(values()).filter(stamp -> stamp.code().equals(code)).findFirst();
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(Stamp::code).toList();
    }
}
