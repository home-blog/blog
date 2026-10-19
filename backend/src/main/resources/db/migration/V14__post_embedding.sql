-- 글의 뜻 정보 (specs/014 data-model 1, ERD-변경-요청.md E-11)
-- 벡터는 pgvector 대신 real[]로 둔다(D-9): 배포 DB는 공용 인스턴스의 스키마라 확장을 켤 권한이 없을 수 있다.
-- 공개 여부는 저장하지 않는다. 고를 때 글·분류·회원을 다시 본다.

CREATE TABLE post_embedding (
    post_id      BIGINT      NOT NULL,
    model        VARCHAR(50) NOT NULL,
    content_hash CHAR(64)    NOT NULL,
    embedding    REAL[]      NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT post_embedding_pk PRIMARY KEY (post_id),
    CONSTRAINT fk_post_embedding_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
);

COMMENT ON TABLE post_embedding IS '글의 뜻 정보(임베딩)';
COMMENT ON COLUMN post_embedding.model IS '만든 임베딩 모델. 바뀌면 다시 만든다';
COMMENT ON COLUMN post_embedding.content_hash IS '모델 + 제목 + 본문 앞부분의 sha256';
COMMENT ON COLUMN post_embedding.embedding IS '길이 1로 맞춘 벡터 (related.embedding.dimensions)';
