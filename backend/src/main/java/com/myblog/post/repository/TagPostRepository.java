package com.myblog.post.repository;

import com.myblog.common.Visibility;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 같은 태그의 공개 글 목록 (specs/005 T045, contracts 9, FR-015, SC-008).
 * 누가 보든 <b>공개 분류의 공개 글만</b> 찾는다 (PostVisibility.isOpenToEveryone과 같은 조건, 주인이어도 자기 비공개 글은 없음).
 * 004 검색(PostSearchRepository)처럼 post → category → blog를 SQL로 이어 읽는다. 쿼리는 고정이고 값은 파라미터로만 넘긴다.
 */
@Repository
public class TagPostRepository {

    private static final String MATCHES = """
            from post p
            join post_tag pt on pt.post_id = p.post_id
            join tag t on t.tag_id = pt.tag_id
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            where t.name = ? and p.visibility = ? and c.visibility = ?
            """;

    private static final String COUNT = "select count(*) " + MATCHES;

    private static final String PAGE = """
            select p.post_id, c.blog_id, b.name as blog_name, p.title, p.category_id, c.name as category_name,
                   p.created_at, p.content
            """ + MATCHES + """
            order by p.created_at desc, p.post_id desc
            offset ? limit ?
            """;

    private final JdbcTemplate jdbc;

    public TagPostRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count(String tagName) {
        Long count = jdbc.queryForObject(COUNT, Long.class, tagName, Visibility.PUBLIC.value(), Visibility.PUBLIC.value());
        return count == null ? 0 : count;
    }

    /** 최신순으로 offset부터 limit개. */
    public List<PostSearchRepository.Match> find(String tagName, long offset, int limit) {
        return jdbc.query(PAGE, (rs, rowNum) -> new PostSearchRepository.Match(rs.getLong("post_id"), rs.getLong("blog_id"),
                        rs.getString("blog_name"), rs.getString("title"), rs.getLong("category_id"),
                        rs.getString("category_name"), rs.getTimestamp("created_at").toInstant(), rs.getString("content")),
                tagName, Visibility.PUBLIC.value(), Visibility.PUBLIC.value(), offset, limit);
    }
}
