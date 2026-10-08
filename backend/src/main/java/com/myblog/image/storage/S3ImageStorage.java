package com.myblog.image.storage;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.image.config.ImageProperties;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3 방식 저장소 (specs/005 T051, D-1: 개발은 MinIO). 접속값은 community.image.s3.*(키는 환경 변수)에서 읽는다.
 * 서버가 켜질 때는 저장소에 연결하지 않는다: 저장소가 꺼져 있어도 서버는 켜지고 이미지 요청만 503이다 (S-8의 8).
 * 버킷은 처음 쓸 때 없으면 만든다. 주소 모양은 MinIO에 맞춰 경로 방식(http://host:9000/버킷/키)을 쓴다.
 */
@Component
@ConditionalOnProperty(prefix = "community.image", name = "storage", havingValue = "s3", matchIfMissing = true)
public class S3ImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorage.class);

    private final S3Client client;
    private final String bucket;
    private final AtomicBoolean bucketReady = new AtomicBoolean();

    public S3ImageStorage(ImageProperties properties) {
        ImageProperties.S3 s3 = properties.s3();
        this.bucket = s3.bucket();
        this.client = S3Client.builder()
                .endpointOverride(URI.create(s3.endpoint()))
                .region(Region.of(s3.region()))
                .forcePathStyle(true)
                // 키가 비어 있어도 서버는 켜진다. 요청할 때 확인해 STORAGE_UNAVAILABLE로 답한다
                .credentialsProvider(() -> AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()))
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                .build();
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        call(() -> {
            ensureBucket();
            return client.putObject(request -> request.bucket(bucket).key(key).contentType(contentType).contentLength(size),
                    RequestBody.fromInputStream(content, size));
        });
    }

    @Override
    public Optional<StoredFile> open(String key) {
        return call(() -> {
            try {
                ResponseInputStream<GetObjectResponse> stream = client.getObject(request -> request.bucket(bucket).key(key));
                return Optional.of(new StoredFile(stream, stream.response().contentLength()));
            } catch (NoSuchKeyException | NoSuchBucketException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public void delete(String key) {
        call(() -> client.deleteObject(request -> request.bucket(bucket).key(key)));
    }

    @Override
    public List<StoredObject> list(String prefix) {
        return call(() -> {
            List<StoredObject> objects = new ArrayList<>();
            try {
                for (S3Object object : client.listObjectsV2Paginator(request -> request.bucket(bucket).prefix(prefix)).contents()) {
                    objects.add(new StoredObject(object.key(), object.lastModified()));
                }
            } catch (NoSuchBucketException e) {
                return List.of();
            }
            return objects;
        });
    }

    private void ensureBucket() {
        if (bucketReady.get()) {
            return;
        }
        try {
            client.headBucket(request -> request.bucket(bucket));
        } catch (NoSuchBucketException e) {
            client.createBucket(request -> request.bucket(bucket));
        }
        bucketReady.set(true);
    }

    /** 저장소 오류는 모두 503 STORAGE_UNAVAILABLE로 바꾼다. 자세한 내용은 로그에만 남긴다. */
    private static <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("이미지 저장소에 연결할 수 없습니다", e);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }
}
