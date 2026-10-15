package com.myblog.post.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 연재에 든 글 (series_post 표, specs/013 data-model 3). 글 하나는 연재 하나에만 든다(post_id 고유).
 * 화 번호는 저장하지 않고 sort_order 순서로 보이는 글만 세어 정한다 (D-10).
 */
@Repository
public class SeriesPostRepository {

    private final JdbcTemplate jdbc;

    public SeriesPostRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 연재의 글을 화 순서로. 공개 규칙은 서비스가 PostVisibility로 거른다. */
    public List<Entry> entries(Long seriesId) {
        return jdbc.query("""
                select sp.post_id, p.title, p.visibility, p.category_id, p.created_at
                from series_post sp join post p on p.post_id = sp.post_id
                where sp.series_id = ?
                order by sp.sort_order, sp.post_id
                """, (rs, i) -> new Entry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                rs.getTimestamp(5).toInstant()), seriesId);
    }

    /** 여러 연재의 글을 한 번에 (블로그의 연재 목록에서 보이는 글 수를 셀 때). */
    public List<SeriesEntry> entriesOf(List<Long> seriesIds) {
        if (seriesIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(connection -> {
            var statement = connection.prepareStatement("""
                    select sp.series_id, p.visibility, p.category_id
                    from series_post sp join post p on p.post_id = sp.post_id
                    where sp.series_id = any(?)
                    """);
            statement.setArray(1, connection.createArrayOf("bigint", seriesIds.toArray()));
            return statement;
        }, (rs, i) -> new SeriesEntry(rs.getLong(1), rs.getString(2), rs.getLong(3)));
    }

    public Optional<Long> seriesOf(Long postId) {
        return jdbc.query("select series_id from series_post where post_id = ?", (rs, i) -> rs.getLong(1), postId)
                .stream().findFirst();
    }

    public int count(Long seriesId) {
        Integer count = jdbc.queryForObject("select count(*) from series_post where series_id = ?", Integer.class, seriesId);
        return count == null ? 0 : count;
    }

    /** 연재의 맨 끝 화로 넣는다. 다른 연재에 있었으면 먼저 뺀다. */
    public void append(Long seriesId, Long postId) {
        jdbc.update("delete from series_post where post_id = ?", postId);
        jdbc.update("""
                insert into series_post (series_id, post_id, sort_order)
                values (?, ?, (select coalesce(max(sort_order), 0) + 1 from series_post where series_id = ?))
                """, seriesId, postId, seriesId);
    }

    public int remove(Long seriesId, Long postId) {
        return jdbc.update("delete from series_post where series_id = ? and post_id = ?", seriesId, postId);
    }

    public void removePost(Long postId) {
        jdbc.update("delete from series_post where post_id = ?", postId);
    }

    public List<Long> postIds(Long seriesId) {
        return jdbc.queryForList("select post_id from series_post where series_id = ?", Long.class, seriesId);
    }

    /** 보낸 순서대로 1, 2, 3 … 을 매긴다. */
    public void reorder(Long seriesId, List<Long> postIds) {
        for (int i = 0; i < postIds.size(); i++) {
            jdbc.update("update series_post set sort_order = ? where series_id = ? and post_id = ?", i + 1, seriesId,
                    postIds.get(i));
        }
    }

    public record Entry(Long postId, String title, String visibility, Long categoryId, Instant createdAt) {
    }

    public record SeriesEntry(Long seriesId, String visibility, Long categoryId) {
    }
}
