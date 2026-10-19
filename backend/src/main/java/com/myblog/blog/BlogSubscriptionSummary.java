package com.myblog.blog;

/**
 * 블로그 머리의 받아 보는 사람 수와 "내가 받아 보나"를 묻는 틀 (specs/016 US1). 구독·알림 모듈(subscription)이 채운다.
 * 채우는 쪽이 없으면 0, false로 본다.
 */
public interface BlogSubscriptionSummary {

    /** viewerId는 로그인하지 않았으면 null (그때 subscribedByMe는 거짓). */
    Summary summary(Long blogId, Long viewerId);

    record Summary(long subscriberCount, boolean subscribedByMe) {

        public static final Summary NONE = new Summary(0, false);
    }
}
