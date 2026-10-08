package com.myblog.image.storage;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 이미지 파일 저장소 틀 (specs/005 T050, research A "저장소 감싸기", D-1).
 * 개발은 MinIO(S3ImageStorage), 설정 community.image.storage=disk면 서버 디스크(DiskImageStorage). 바깥 주소는 같다(/api/images/…).
 * 저장소에 연결할 수 없으면 구현이 503 STORAGE_UNAVAILABLE(ApiException)로 바꿔 던진다. 경로나 저장소 주소는 응답에 넣지 않는다.
 * key는 서버가 만든 이름(posts/{UUID}.{확장자})이다. 사용자가 준 파일 이름은 쓰지 않는다.
 */
public interface ImageStorage {

    void put(String key, InputStream content, long size, String contentType);

    /** 없으면 비어 있다. 받은 쪽이 스트림을 닫는다. */
    Optional<StoredFile> open(String key);

    /** 없어도 오류가 아니다. */
    void delete(String key);

    /** prefix로 시작하는 모든 파일 (정리 작업이 DB에 없는 파일을 찾을 때, D-5). */
    List<StoredObject> list(String prefix);

    record StoredFile(InputStream content, long size) {
    }

    record StoredObject(String key, Instant lastModified) {
    }
}
