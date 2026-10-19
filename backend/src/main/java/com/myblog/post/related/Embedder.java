package com.myblog.post.related;

import java.util.List;

/**
 * 글을 뜻 벡터(임베딩)로 바꾼다 (specs/014 D-9, D-10, D-15). Voyage AI, 개발용 가짜, 꺼짐 가운데 설정에 따라 하나가 빈으로 뜬다.
 * 돌려주는 벡터는 길이 1로 맞춰져 있어 내적이 곧 코사인 유사도다.
 */
public interface Embedder {

    /** 저장할 때 함께 적는 모델 이름(차원 포함, 50자 이하). 이것이 바뀌면 다시 만든다. */
    String model();

    boolean available();

    /** 글(문서)들의 벡터. 순서는 넣은 순서. 실패하면 {@link UnavailableException}. */
    List<float[]> embed(List<String> documents);

    class UnavailableException extends RuntimeException {

        public UnavailableException(String message) {
            super(message);
        }

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
