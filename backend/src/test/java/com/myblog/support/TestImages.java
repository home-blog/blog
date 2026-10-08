package com.myblog.support;

import com.myblog.image.storage.InMemoryImageStorage;
import java.util.Arrays;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 005 이미지 테스트 도우미: 메모리 저장소를 진짜 저장소 대신 쓰게 하고, 형식 표시(시그니처)가 맞는 가짜 이미지 파일을 만든다.
 * 이 설정을 가져오는 테스트끼리 같은 Spring 컨텍스트를 쓴다.
 */
@TestConfiguration
public class TestImages {

    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    public static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    public static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a'};
    public static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    public static final byte[] BMP = {'B', 'M', 0, 0};

    @Bean
    @Primary
    InMemoryImageStorage inMemoryImageStorage() {
        return new InMemoryImageStorage();
    }

    /** signature로 시작하고 전체가 size바이트인 파일. */
    public static byte[] bytes(byte[] signature, int size) {
        byte[] data = Arrays.copyOf(signature, Math.max(size, signature.length));
        Arrays.fill(data, signature.length, data.length, (byte) 7);
        return data;
    }

    public static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    public static MockMultipartFile png() {
        return file("photo.png", "image/png", bytes(PNG, 64));
    }
}
