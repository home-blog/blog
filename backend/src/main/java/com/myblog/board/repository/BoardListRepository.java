package com.myblog.board.repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 글마당 목록·검색 (specs/017 contracts 1, D-8).
 * <p>
 * 쿼리는 고정된 두 개(세기, 한 쪽 읽기)뿐이다. 고르기(주제·종류·해결 전)는 비어 있으면 조건이 없는 것과 같게 값으로 넘기고,
 * 검색 단어 묶음은 배열 값 하나로 넘긴다. 문자열을 이어 붙여 쿼리를 만들지 않는다 (헌법 IV).
 * 검색 조건은 "제목·본문·주제 이름·쓴 사람 닉네임 어디에도 없는 단어가 하나도 없다" = 모든 단어가 어딘가에 있다(대소문자 무시).
 * 탈퇴한 회원의 바뀐 닉네임(탈퇴한사용자N)으로는 찾지 않는다. 주제·회원 표는 SQL로만 읽는다(글 검색과 같은 방식).
 */
@Repository
public class BoardListRepository {

    private static final String MATCHES = """
            from board_post p
            join topic t on t.topic_id = p.topic_id
            join users u on u.users_id = p.users_id
            where (?::bigint is null or p.topic_id = ?)
              and (?::text is null or p.kind = ?)
              and (not ? or (p.kind = 'question' and p.accepted_comment_id is null))
              and not exists (
                select 1 from unnest(?::text[]) as w(pattern)
                where not (p.title ilike w.pattern escape '\\' or p.content ilike w.pattern escape '\\'
                           or t.topic_name ilike w.pattern escape '\\'
                           or (u.deleted_at is null and u.nickname ilike w.pattern escape '\\')))
            """;

    private static final String COUNT = "select count(*) " + MATCHES;

    private static final String PAGE = """
            select p.board_post_id, t.topic_name, p.kind, p.title, p.users_id, u.nickname, u.deleted_at,
                   p.created_at, p.views, p.comment_count, p.accepted_comment_id
            """ + MATCHES + """
            order by p.created_at desc, p.board_post_id desc
            offset ? limit ?
            """;

    private final JdbcTemplate jdbc;

    public BoardListRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** topicId·kind는 비어 있으면(null) 고르지 않는다. patterns는 단어마다 %단어% (비어 있으면 찾지 않는다). */
    public record Filter(Long topicId, String kind, boolean unsolvedOnly, List<String> patterns) {

        public static Filter none() {
            return new Filter(null, null, false, List.of());
        }
    }

    public long count(Filter filter) {
        Long count = jdbc.query(connection -> prepare(connection, COUNT, filter), rs -> rs.next() ? rs.getLong(1) : 0L);
        return count == null ? 0 : count;
    }

    /** 최신순으로 offset부터 limit개. */
    public List<Row> find(Filter filter, long offset, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = prepare(connection, PAGE, filter);
            statement.setLong(7, offset);
            statement.setInt(8, limit);
            return statement;
        }, (rs, rowNum) -> new Row(rs.getLong("board_post_id"), rs.getString("topic_name"), rs.getString("kind"),
                rs.getString("title"), rs.getLong("users_id"),
                rs.getTimestamp("deleted_at") == null ? rs.getString("nickname") : null,
                rs.getTimestamp("deleted_at") != null, rs.getTimestamp("created_at").toInstant(), rs.getLong("views"),
                rs.getInt("comment_count"), rs.getObject("accepted_comment_id") != null));
    }

    private static PreparedStatement prepare(Connection connection, String sql, Filter filter) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        if (filter.topicId() == null) {
            statement.setNull(1, Types.BIGINT);
            statement.setNull(2, Types.BIGINT);
        } else {
            statement.setLong(1, filter.topicId());
            statement.setLong(2, filter.topicId());
        }
        statement.setString(3, filter.kind());
        statement.setString(4, filter.kind());
        statement.setBoolean(5, filter.unsolvedOnly());
        Array words = connection.createArrayOf("text", filter.patterns().toArray());
        statement.setArray(6, words);
        return statement;
    }

    /** 목록 한 줄. 쓴 사람이 탈퇴했으면 nickname이 비고 withdrawn이 참. solved는 채택한 댓글이 있는 물음. */
    public record Row(Long boardPostId, String topic, String kind, String title, Long writerId, String nickname,
            boolean withdrawn, Instant createdAt, long views, int commentCount, boolean solved) {
    }
}
