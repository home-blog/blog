package com.myblog.post;

import java.util.List;
import java.util.Optional;

/**
 * 서비스 전체의 주제(여행, 음식, 취미, 운동, 개발)를 다른 모듈에 알려 주는 입구 (specs/017 D-2).
 * 글마당은 블로그 글과 같은 주제 표를 쓴다. 주제 표는 글 모듈 것이라 글마당 모듈은 이것으로만 묻는다.
 */
public interface TopicCatalog {

    /** 정해진 순서(sort_order)로 모든 주제. */
    List<TopicRef> all();

    /** 이름이 같은 주제. 없으면 비운다. */
    default Optional<TopicRef> byName(String name) {
        return name == null ? Optional.empty() : all().stream().filter(topic -> topic.name().equals(name)).findFirst();
    }

    record TopicRef(Long id, String name) {
    }
}
