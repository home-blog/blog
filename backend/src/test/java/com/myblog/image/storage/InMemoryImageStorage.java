package com.myblog.image.storage;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트용 메모리 저장소 (specs/005 T050). 저장소가 꺼진 것(failAll)과 파일 삭제만 실패하는 것(failDelete)을 흉내 낸다.
 * 파일의 마지막 수정 시각을 바꿔 정리 작업의 "오래된 파일"을 만든다.
 */
public class InMemoryImageStorage implements ImageStorage {

    private final Map<String, Entry> files = new ConcurrentHashMap<>();
    public volatile boolean failAll;
    public volatile boolean failDelete;

    public record Entry(byte[] bytes, String contentType, Instant lastModified) {
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        checkUp();
        try {
            files.put(key, new Entry(content.readAllBytes(), contentType, Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<StoredFile> open(String key) {
        checkUp();
        Entry entry = files.get(key);
        return entry == null ? Optional.empty()
                : Optional.of(new StoredFile(new ByteArrayInputStream(entry.bytes()), entry.bytes().length));
    }

    @Override
    public void delete(String key) {
        checkUp();
        if (failDelete) {
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
        files.remove(key);
    }

    @Override
    public List<StoredObject> list(String prefix) {
        checkUp();
        return files.entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
                .map(e -> new StoredObject(e.getKey(), e.getValue().lastModified())).toList();
    }

    public boolean has(String key) {
        return files.containsKey(key);
    }

    public Entry get(String key) {
        return files.get(key);
    }

    /** 저장소에만 있는 파일을 둔다 (기록 없는 파일). */
    public void putRaw(String key, byte[] bytes, Instant lastModified) {
        files.put(key, new Entry(bytes, "image/png", lastModified));
    }

    public void age(String key, Instant lastModified) {
        files.computeIfPresent(key, (k, entry) -> new Entry(entry.bytes(), entry.contentType(), lastModified));
    }

    public void reset() {
        failAll = false;
        failDelete = false;
    }

    private void checkUp() {
        if (failAll) {
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }
}
