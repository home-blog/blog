package com.myblog.image.storage;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.image.config.ImageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서버 디스크 저장소 (specs/005 T051, D-1의 교체용). community.image.storage=disk, 폴더는 community.image.disk.directory.
 * key(posts/{UUID}.{확장자})를 폴더 아래 경로로 쓴다. 폴더 밖을 가리키는 key는 받지 않는다.
 */
@Component
@ConditionalOnProperty(prefix = "community.image", name = "storage", havingValue = "disk")
public class DiskImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(DiskImageStorage.class);

    private final Path root;

    public DiskImageStorage(ImageProperties properties) {
        this.root = Path.of(properties.disk().directory()).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        Path target = resolve(key);
        run(() -> {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), ".upload", ".tmp");
            try {
                Files.copy(content, temp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
            return null;
        });
    }

    @Override
    public Optional<StoredFile> open(String key) {
        Path target = resolve(key);
        return run(() -> {
            try {
                return Optional.of(new StoredFile(Files.newInputStream(target), Files.size(target)));
            } catch (NoSuchFileException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public void delete(String key) {
        Path target = resolve(key);
        run(() -> Files.deleteIfExists(target));
    }

    @Override
    public List<StoredObject> list(String prefix) {
        return run(() -> {
            if (!Files.isDirectory(root)) {
                return List.of();
            }
            try (Stream<Path> files = Files.walk(root)) {
                return files.filter(Files::isRegularFile)
                        .map(path -> root.relativize(path).toString().replace('\\', '/'))
                        .filter(key -> key.startsWith(prefix) && !key.endsWith(".tmp"))
                        .map(key -> new StoredObject(key, lastModified(resolve(key))))
                        .toList();
            }
        });
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("저장소 폴더 밖의 이름입니다");
        }
        return path;
    }

    private static java.time.Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T run(IoAction<T> action) {
        try {
            return action.run();
        } catch (IOException | UncheckedIOException e) {
            log.error("이미지 폴더를 쓸 수 없습니다", e);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @FunctionalInterface
    private interface IoAction<T> {
        T run() throws IOException;
    }
}
