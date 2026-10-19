package com.myblog.post.related;

import java.util.List;

/**
 * 개발용 가짜 임베딩 (specs/014 D-15): 열쇠 없이 화면을 돌려 보려고, 글자 두 개씩 묶은 조각을 세어 벡터를 만든다.
 * 같은 낱말을 많이 나눠 가진 글이 가깝다. 뜻은 모르지만 같은 글은 늘 같은 벡터다.
 */
public class FakeEmbedder implements Embedder {

    private final int dimensions;

    public FakeEmbedder(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String model() {
        return "fake";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<float[]> embed(List<String> documents) {
        return documents.stream().map(this::embedOne).toList();
    }

    private float[] embedOne(String text) {
        float[] vector = new float[dimensions];
        String compact = text.replaceAll("\\s+", " ").strip();
        for (int i = 0; i + 1 < compact.length(); i++) {
            String gram = compact.substring(i, i + 2);
            if (gram.isBlank()) {
                continue;
            }
            vector[Math.floorMod(gram.hashCode(), dimensions)] += 1;
        }
        return Vectors.normalize(vector);
    }
}
