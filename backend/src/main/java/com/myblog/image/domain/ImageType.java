package com.myblog.image.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 받는 이미지 형식 (FR-022). 형식은 파일 앞부분의 형식 표시(시그니처)로만 정한다 (D-6 B).
 * 확장자와 브라우저가 보낸 형식은 믿지 않는다. 저장 확장자와 내려 줄 Content-Type은 확인한 형식으로 정한다. SVG는 받지 않는다.
 */
public enum ImageType {
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    WEBP("webp", "image/webp");

    /** 형식을 알아보려면 앞에서 이만큼 읽는다 (webp가 가장 길다: RIFF????WEBP). */
    public static final int HEADER_LENGTH = 12;

    private final String extension;
    private final String contentType;

    ImageType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** 확장자(jpg 등)로 찾는다. 저장된 이름의 확장자는 올릴 때 확인한 형식이다. */
    public static Optional<ImageType> fromExtension(String extension) {
        return Arrays.stream(values()).filter(type -> type.extension.equalsIgnoreCase(extension)).findFirst();
    }

    /**
     * 파일 앞부분(header, 앞에서 읽은 만큼)으로 형식을 알아본다.
     * jpg FF D8 FF, png 89 50 4E 47 0D 0A 1A 0A, gif "GIF87a"/"GIF89a", webp "RIFF" + 4바이트 + "WEBP".
     */
    public static Optional<ImageType> detect(byte[] header, int length) {
        if (startsWith(header, length, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPG);
        }
        if (startsWith(header, length, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (startsWith(header, length, 'G', 'I', 'F', '8', '7', 'a') || startsWith(header, length, 'G', 'I', 'F', '8', '9', 'a')) {
            return Optional.of(GIF);
        }
        if (startsWith(header, length, 'R', 'I', 'F', 'F') && length >= 12
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] header, int length, int... expected) {
        if (length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
