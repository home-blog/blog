package com.myblog.image.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.image.config.ImageProperties;
import com.myblog.image.domain.ImageType;
import com.myblog.image.domain.PostImage;
import com.myblog.image.repository.PostImageRepository;
import com.myblog.image.storage.ImageStorage;
import com.myblog.post.PostLookup;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 올리기 (specs/005 T055, contracts 10, research B-5, FR-022 ~ FR-025).
 * <ol>
 *   <li>postId가 있으면(수정 중) <b>내 글</b>인지 본다. 아니면 POST_NOT_FOUND.</li>
 *   <li>크기: max-size(5MB) 이하. 정확히 5MB는 통과. 넘으면 INVALID_IMAGE.</li>
 *   <li>형식: 파일 앞부분의 형식 표시로 jpg·png·gif·webp인지 (D-6 B). 이름·브라우저 정보는 믿지 않는다.</li>
 *   <li>개수: 새 글이면 내 연결 전 이미지 수, 수정 중이면 그 글의 이미지 수 + 내 연결 전 이미지 수가 max-per-post(10)면 IMAGE_LIMIT_EXCEEDED.
 *       같은 회원의 올리기는 트랜잭션 잠금으로 한 줄로 세워 동시에 와도 넘지 않는다.</li>
 *   <li>서버가 새 이름(posts/{UUID}.{확인한 확장자})을 만들어 저장소에 넣고 기록한다. 기록이 실패하면 방금 넣은 파일을 지운다.</li>
 * </ol>
 * 기록은 늘 "아직 글 없음"으로 남긴다. 글에 연결하는 것은 글을 저장할 때다 (ImageLinker, D-3).
 */
@Service
public class ImageUploadService {

    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;

    private final PostImageRepository images;
    private final ImageStorage storage;
    private final PostLookup postLookup;
    private final ImageProperties properties;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ImageFileRemover fileRemover;
    private final Clock clock;

    public ImageUploadService(PostImageRepository images, ImageStorage storage, PostLookup postLookup,
            ImageProperties properties, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            ImageFileRemover fileRemover, Clock clock) {
        this.images = images;
        this.storage = storage;
        this.postLookup = postLookup;
        this.properties = properties;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.fileRemover = fileRemover;
        this.clock = clock;
    }

    public Uploaded upload(Long memberId, MultipartFile file, Long postId) {
        if (postId != null) {
            postLookup.findVisible(postId, memberId).filter(post -> post.isOwnedBy(memberId))
                    .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        }
        if (file == null || file.isEmpty() || file.getSize() > properties.maxSize().toBytes()) {
            throw invalidImage();
        }
        ImageType type = detect(file).filter(properties.allowedImageTypes()::contains).orElseThrow(this::invalidImage);
        String fileName = UUID.randomUUID() + "." + type.extension();
        PostImage saved = transaction.execute(status -> {
            jdbc.query(MEMBER_LOCK_SQL, IGNORE, "image:" + memberId);
            long count = images.countUnlinked(memberId) + (postId == null ? 0 : images.countByPostId(postId));
            if (count >= properties.maxPerPost()) {
                throw new ApiException(ErrorCode.IMAGE_LIMIT_EXCEEDED, limitMessage(properties.maxPerPost()));
            }
            PostImage image = PostImage.uploaded(memberId, fileName, Instant.now(clock));
            try (InputStream content = file.getInputStream()) {
                storage.put(image.getStorageKey(), content, file.getSize(), type.contentType());
            } catch (IOException e) {
                throw invalidImage();
            }
            try {
                return images.saveAndFlush(image);
            } catch (RuntimeException e) {
                fileRemover.removeQuietly(List.of(image.getStorageKey()));
                throw e;
            }
        });
        return new Uploaded(saved.getId(), ImageUrls.of(saved.fileName()));
    }

    private static Optional<ImageType> detect(MultipartFile file) {
        byte[] header = new byte[ImageType.HEADER_LENGTH];
        try (InputStream in = file.getInputStream()) {
            int length = in.readNBytes(header, 0, header.length);
            return ImageType.detect(header, length);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private ApiException invalidImage() {
        String types = String.join(", ", properties.allowedTypes());
        return new ApiException(ErrorCode.INVALID_IMAGE,
                "이미지는 %dMB 이하의 %s만 올릴 수 있습니다".formatted(properties.maxSize().toMegabytes(), types));
    }

    static String limitMessage(int maxPerPost) {
        return "※ 이미지는 글 하나에 %d장까지 올릴 수 있습니다".formatted(maxPerPost);
    }

    /** url은 본문에 넣을 우리 서버 주소다 (D-4). */
    public record Uploaded(Long imageId, String url) {
    }
}
