package com.myblog.post.related;

/** 벡터 계산 (길이 1로 맞추기, 내적). */
public final class Vectors {

    private Vectors() {
    }

    /** 길이 1로 맞춘 새 배열. 0 벡터는 그대로. */
    public static float[] normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += (double) v * v;
        }
        float[] out = vector.clone();
        if (sum == 0) {
            return out;
        }
        float length = (float) Math.sqrt(sum);
        for (int i = 0; i < out.length; i++) {
            out[i] /= length;
        }
        return out;
    }

    /**
     * 임베딩 서비스가 준 숫자를 저장할 수 있는 벡터로 바꾼다: 하나라도 float로 나타낼 수 없거나(무한대·NaN) 모두 0이면 null.
     * 그런 벡터는 비슷한 정도가 NaN이나 0이 되어 비슷한 글 순서를 망가뜨린다.
     */
    public static float[] usable(java.util.List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            Double value = values.get(i);
            if (value == null || !Float.isFinite(value.floatValue())) {
                return null;
            }
            vector[i] = value.floatValue();
        }
        float[] normalized = normalize(vector);
        float length = dot(normalized, normalized);
        return Float.isFinite(length) && length > 0 ? normalized : null;
    }

    /** 내적. 둘 다 길이 1이면 코사인 유사도. 길이가 다르면 짧은 쪽까지만. */
    public static float dot(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += (double) a[i] * b[i];
        }
        return (float) sum;
    }
}
