package com.myblog.post;

/**
 * 글을 저장했다 (새 글, 수정) (specs/005 T057, D-3, D-4). 글 모듈이 글을 저장한 <b>같은 트랜잭션 안에서</b> 낸다.
 * 듣는 쪽: image(ImageLinker)가 본문 속 우리 이미지 주소(/api/images/…)를 찾아 이 글에 연결하고, 빠진 이미지는 지운다.
 * 듣는 쪽이 거절하면(예: 이미지 10장 초과) 글 저장 전체가 취소된다.
 *
 * @param content 저장한 본문 (마크다운 원문)
 */
public record PostContentSavedEvent(Long postId, Long ownerId, String content) {
}
