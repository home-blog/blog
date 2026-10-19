/**
 * 글마당 모듈 (specs/017): 블로그와 따로 서는 주제별 게시판. 이야기·물음 글, 댓글, 물음의 채택 도장.
 * 주제는 글 모듈의 맨 위 틀(TopicCatalog), 쓴 사람 이름은 회원 모듈의 MemberNames, 관리자 여부는 LoggedInMember로만 묻는다
 * (user ← post ← board). 위 모듈은 이 모듈의 맨 위 타입만 쓴다: 조회 수는 통계 모듈이 BoardPostViewedEvent를 듣고
 * BoardViewCounter로 올리고, 우편함 소식은 구독 모듈이 BoardCommentWrittenEvent·BoardAnswerAcceptedEvent를 듣는다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "글마당", allowedDependencies = {"post", "user", "common"})
package com.myblog.board;
