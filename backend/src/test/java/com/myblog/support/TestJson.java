package com.myblog.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** 테스트 요청 본문을 만드는 작은 도우미: TestJson.of("title", "제목", "categoryId", 3). 값이 null이면 null로 쓴다. */
public final class TestJson {

    private TestJson() {
    }

    public static String of(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        StringBuilder sb = new StringBuilder("{");
        map.forEach((key, value) -> {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(quote(key)).append(':').append(value == null ? "null"
                    : value instanceof String s ? quote(s) : value.toString());
        });
        return sb.append('}').toString();
    }

    /** 글자 하나를 JSON 문자열(따옴표 포함)로 바꾼다. */
    public static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
