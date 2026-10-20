package com.myblog.post.repository;

import com.myblog.common.Visibility;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * AI 비서 연결의 글 찾기 (specs/020 T005). 보는 조건은 PostVisibility와 같다: 공개 분류의 공개 글이거나 내 블로그 글.
 * 단어 묶음은 배열 값 하나로 넘긴다. 문자열을 이어 붙여 쿼리를 만들지 않는다(헌법 IV). 쿼리는 고정된 두 개:
 * 모든 낱말이 있는 글(ALL, 글 찾기)과 낱말이 하나라도 있는 글(ANY, 단락 찾기의 후보).
 */
@Repository
public class PostLibraryRepository {

    private static final String SELECT = """
            select p.post_id, p.title, p.content, p.visibility, p.created_at, b.name as blog_name
            from post p
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            where ((p.visibility = ? and c.visibility = ?) or b.users_id = ?)
              and (? = false or b.users_id = ?)
            """;

    private static final String ALL_WORDS = SELECT + """
              and not exists (
                select 1 from unnest(?::text[]) as w(pattern)
                where not (p.title ilike w.pattern escape '\\' or p.content ilike w.pattern escape '\\'))
            order by p.created_at desc, p.post_id desc
            limit ?
            """;

    private static final String ANY_WORD = SELECT + """
              and exists (
                select 1 from unnest(?::text[]) as w(pattern)
                where p.title ilike w.pattern escape '\\' or p.content ilike w.pattern escape '\\')
            order by p.created_at desc, p.post_id desc
            limit ?
            """;

    private static final String ONE = SELECT + "  and p.post_id = ?";

    private final JdbcTemplate jdbc;

    public PostLibraryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 모든 낱말 모양(%낱말%)에 맞는 읽을 수 있는 글, 최근 순. viewerId는 로그인한 회원(열쇠의 주인). */
    public List<Found> findAll(Long viewerId, boolean mineOnly, List<String> patterns, int limit) {
        return find(ALL_WORDS, viewerId, mineOnly, patterns, limit);
    }

    /** 낱말이 하나라도 맞는 읽을 수 있는 글, 최근 순 (단락 찾기 후보). */
    public List<Found> findAny(Long viewerId, boolean mineOnly, List<String> patterns, int limit) {
        return find(ANY_WORD, viewerId, mineOnly, patterns, limit);
    }

    /** 읽을 수 있는 글 하나 (글 읽기). */
    public Optional<Found> findReadable(Long viewerId, Long postId) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(ONE);
            statement.setString(1, Visibility.PUBLIC.value());
            statement.setString(2, Visibility.PUBLIC.value());
            statement.setLong(3, viewerId);
            statement.setBoolean(4, false);
            statement.setLong(5, viewerId);
            statement.setLong(6, postId);
            return statement;
        }, (rs, rowNum) -> new Found(rs.getLong("post_id"), rs.getString("title"), rs.getString("content"),
                rs.getString("visibility"), rs.getTimestamp("created_at").toInstant(), rs.getString("blog_name")))
                .stream().findFirst();
    }

    private List<Found> find(String sql, Long viewerId, boolean mineOnly, List<String> patterns, int limit) {
        return jdbc.query(connection -> prepare(connection, sql, viewerId, mineOnly, patterns, limit),
                (rs, rowNum) -> new Found(rs.getLong("post_id"), rs.getString("title"), rs.getString("content"),
                        rs.getString("visibility"), rs.getTimestamp("created_at").toInstant(), rs.getString("blog_name")));
    }

    private static PreparedStatement prepare(Connection connection, String sql, Long viewerId, boolean mineOnly,
            List<String> patterns, int limit) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        Array words = connection.createArrayOf("text", patterns.toArray());
        statement.setString(1, Visibility.PUBLIC.value());
        statement.setString(2, Visibility.PUBLIC.value());
        statement.setLong(3, viewerId);
        statement.setBoolean(4, mineOnly);
        statement.setLong(5, viewerId);
        statement.setArray(6, words);
        statement.setInt(7, limit);
        return statement;
    }

    public record Found(Long postId, String title, String content, String visibility, Instant createdAt,
            String blogName) {
    }
}
