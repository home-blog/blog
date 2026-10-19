package com.myblog.post;

import java.util.List;

/**
 * 이 사람이 받아 보는 블로그 번호들을 묻는 틀 (specs/016 US1 "받아 보는 글"). 구독·알림 모듈(subscription)이 채운다.
 * 채우는 쪽이 없으면 빈 목록으로 본다.
 */
public interface SubscribedBlogs {

    List<Long> blogIdsOf(Long memberId);
}
