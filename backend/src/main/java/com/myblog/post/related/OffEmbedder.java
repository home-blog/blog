package com.myblog.post.related;

import java.util.List;

/** 열쇠가 없어 꺼진 임베딩 (specs/014 D-15). 비슷한 글은 태그·주제로 대신 고른다. */
public class OffEmbedder implements Embedder {

    @Override
    public String model() {
        return "off";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<float[]> embed(List<String> documents) {
        throw new UnavailableException("임베딩이 꺼져 있습니다");
    }
}
