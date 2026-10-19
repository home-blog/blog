package com.myblog.board.domain;

import java.util.Arrays;
import java.util.Optional;

/** 글마당 글의 종류 (specs/017): 이야기(talk)와 물음(question). 물음만 댓글을 채택할 수 있다. */
public enum BoardKind {
    TALK("talk"),
    QUESTION("question");

    private final String value;

    BoardKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<BoardKind> from(String value) {
        return Arrays.stream(values()).filter(kind -> kind.value.equals(value)).findFirst();
    }
}
