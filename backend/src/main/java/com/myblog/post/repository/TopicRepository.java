package com.myblog.post.repository;

import com.myblog.post.domain.Topic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findAllByOrderBySortOrderAscIdAsc();
}
