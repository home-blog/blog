/**
 * 이미지 모듈: 글 이미지 올리기·보기, 글에 연결, 정리 (specs/005 US4).
 * 글은 글 모듈의 맨 위 타입(PostLookup, PostContentSavedEvent, PostDeletingEvent)으로만 묻고 듣는다.
 * 글 모듈은 이 모듈을 부르지 않는다 (user ← blog ← post ← image). 파일은 ImageStorage 뒤에 둔다:
 * 개발은 MinIO(S3 방식), 설정 하나(community.image.storage)로 서버 디스크로 바꿔 끼울 수 있다 (D-1).
 */
@org.springframework.modulith.ApplicationModule(displayName = "이미지", allowedDependencies = {"post", "user", "common"})
package com.myblog.image;
