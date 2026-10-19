package com.myblog.board;

import java.util.Collection;
import java.util.Set;

/** 글마당 글이 아직 있는지 알려 주는 입구 (specs/017 D-9). 우편함이 지운 글의 소식을 "볼 수 없는 글"로 보인다. */
public interface BoardPosts {

    /** 받은 번호 가운데 지금 있는 글의 번호. */
    Set<Long> existing(Collection<Long> boardPostIds);
}
