package com.myblog.support;

import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 004 테스트가 쓰는 글 준비 도우미: 본문과 작성 시각을 정해 표에 바로 넣는다.
 * 같은 시각의 글, 오래된 글처럼 주소로는 만들 수 없는 "이미 있는 데이터"를 준비할 때만 쓴다.
 */
@Component
public class TestPosts {

    private final JdbcTemplate jdbc;

    public TestPosts(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long add(Long categoryId, String title, String content, String visibility, Instant createdAt) {
        return jdbc.queryForObject("insert into post (category_id, topic_id, title, content, visibility, created_at)"
                + " values (?, (select min(topic_id) from topic), ?, ?, ?, ?) returning post_id",
                Long.class, categoryId, title, content, visibility, Timestamp.from(createdAt));
    }
}
