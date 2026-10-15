package com.myblog.post;

/**
 * 임시 저장 글을 저장했다 (specs/013 US3, D-8). 글 모듈이 저장한 같은 트랜잭션 안에서 낸다.
 * 듣는 쪽: image(ImageLinker)가 본문 속 이 회원의 연결 전 이미지를 이 임시 저장 글에 붙이고(정리 작업이 지우지 않게),
 * 본문에서 빠진 이미지는 떼어 정리 대상으로 돌린다. 임시 저장 글을 지우면 DB가 떼어 준다(ON DELETE SET NULL).
 *
 * @param content 저장한 본문 (마크다운 원문)
 */
public record DraftContentSavedEvent(Long draftId, Long ownerId, String content) {
}
