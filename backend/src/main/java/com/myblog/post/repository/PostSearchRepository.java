package com.myblog.post.repository;

import com.myblog.common.Visibility;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 공개 글 검색 (specs/004 T028, research B-4, R-1, R-2, D-6, D-7, D-8).
 * <p>
 * 쿼리는 고정된 두 개(세기, 한 페이지 읽기)뿐이고, 단어 묶음은 배열 값 하나로 넘긴다. 문자열을 이어 붙여 쿼리를 만들지 않는다
 * (헌법 IV). 조건은 "제목에도 본문에도 없는 단어가 하나도 없다" = 모든 단어가 제목이나 본문에 있다(대소문자 무시).
 * <p>
 * 누가 검색하든 <b>공개 분류의 공개 글만</b> 찾는다 (PostVisibility.isOpenToEveryone과 같은 조건, 주인 여부는 보지 않음).
 * 블로그 이름은 post → category → blog로 이어 읽는다 (글 표에 블로그 칸이 없다, D-6). 표를 SQL로만 읽으므로
 * 블로그 모듈의 클래스를 부르지 않는다. 검색은 저장된 마크다운 원문 그대로 찾는다 (D-8).
 */
@Repository
public class PostSearchRepository {

    private static final String MATCHES = """
            from post p
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            where p.visibility = ? and c.visibility = ?
              and not exists (
                select 1 from unnest(?::text[]) as w(pattern)
                where not (p.title ilike w.pattern escape '\\' or p.content ilike w.pattern escape '\\'))
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

    public PostSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 모든 단어 모양(%단어%)에 맞는 공개 글 수. */
    public long count(List<String> patterns) {
        Long count = jdbc.query(connection -> prepare(connection, COUNT, patterns), rs -> rs.next() ? rs.getLong(1) : 0L);
        return count == null ? 0 : count;
    }

    /** 최신순으로 offset부터 limit개. */
    public List<Match> find(List<String> patterns, long offset, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = prepare(connection, PAGE, patterns);
            statement.setLong(4, offset);
            statement.setInt(5, limit);
            return statement;
        }, (rs, rowNum) -> new Match(rs.getLong("post_id"), rs.getLong("blog_id"), rs.getString("blog_name"),
                rs.getString("title"), rs.getLong("category_id"), rs.getString("category_name"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("content")));
    }

    private static PreparedStatement prepare(Connection connection, String sql, List<String> patterns)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        Array words = connection.createArrayOf("text", patterns.toArray());
        statement.setString(1, Visibility.PUBLIC.value());
        statement.setString(2, Visibility.PUBLIC.value());
        statement.setArray(3, words);
        return statement;
    }

    /** 찾은 글 한 줄. 본문은 미리보기를 만들 때만 쓴다. */
    public record Match(long postId, long blogId, String blogName, String title, long categoryId,
            String categoryName, Instant createdAt, String content) {
    }
}
