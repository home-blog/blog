package com.myblog.post.service;

import com.myblog.post.TopicCatalog;
import com.myblog.post.repository.TopicRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** TopicCatalog를 주제 표로 채운다 (specs/017 D-2). 주제는 다섯 줄뿐이라 부를 때마다 읽는다. */
@Component
public class TopicCatalogAdapter implements TopicCatalog {

    private final TopicRepository topics;

    public TopicCatalogAdapter(TopicRepository topics) {
        this.topics = topics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicRef> all() {
        return topics.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(topic -> new TopicRef(topic.getId(), topic.getName()))
                .toList();
    }
}
