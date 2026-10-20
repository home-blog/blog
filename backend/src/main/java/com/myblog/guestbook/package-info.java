/**
 * 방명록 모듈 (엽서함, specs/021): 블로그에 들른 회원이 엽서를 남기고, 블로그 주인이 답장한다.
 * 블로그와 주인은 블로그 모듈의 BlogDirectory로, 쓴 사람 이름은 회원 모듈의 MemberNames로만 묻는다 (user ← blog ← guestbook).
 * 위 모듈은 이 모듈의 맨 위 타입만 쓴다: 우편함 소식은 구독 모듈이 GuestbookWrittenEvent·GuestbookRepliedEvent를 커밋 뒤에 듣는다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "방명록", allowedDependencies = {"blog", "user", "common"})
package com.myblog.guestbook;
