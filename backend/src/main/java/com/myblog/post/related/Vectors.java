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
