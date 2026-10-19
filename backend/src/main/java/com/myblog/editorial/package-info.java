/**
 * 편집실 모듈 (specs/018): 관리자가 신고를 검토하고 글을 내리거나(비공개로 돌리고 잠금) 반려하는 곳, 글마당 신고 처리, 운영 현황.
 * 맨 위 모듈이라 다른 모듈의 맨 위 타입(PostModeration, PostReports, BoardReports, LoggedInMember)만 부른다.
 * 아래 모듈은 이 모듈을 모른다. 글 주인에게 가는 우편함 소식은 구독 모듈이 EditorialNoticeEvent를 커밋 뒤에 듣고 만든다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "편집실",
        allowedDependencies = {"board", "community", "post", "user", "common"})
package com.myblog.editorial;
