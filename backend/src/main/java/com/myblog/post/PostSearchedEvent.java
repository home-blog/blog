package com.myblog.post;

/**
 * 공개 글을 검색해 결과가 1개 이상 나왔다 (specs/015 US1, S1-1). 통계 모듈이 듣고 인기 검색어를 센다.
 * 글 모듈은 통계 모듈을 부르지 않는다. 듣는 쪽의 실패는 검색을 망치지 않는다(듣는 쪽이 삼킨다).
 * 검색 트랜잭션이 끝난 뒤(컨트롤러에서) 낸다.
 *
 * @param keyword 서비스가 다듬은 검색어(앞뒤 공백을 지운 것)
 * @param viewerId 로그인하지 않았으면 null (통계 모듈이 방문자 쿠키로 가린다)
 */
public record PostSearchedEvent(String keyword, Long viewerId) {
}
