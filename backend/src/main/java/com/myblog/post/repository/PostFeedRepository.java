package com.myblog.post.repository;

import com.myblog.common.Visibility;
import com.myblog.post.config.ManuscriptProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * 첫 화면 글 모음: 모든 블로그의 공개 글을 최신순으로, 그리고 이번 주 인기 글 (주제를 고르면 그 주제만).
 * <p>
 * 검색(PostSearchRepository)과 같은 조건으로 <b>공개 분류의 공개 글만</b> 읽고, 탈퇴한 회원의 글은 뺀다.
 * 블로그 이름·글쓴이 닉네임·댓글 수·좋아요 수는 표를 SQL로만 읽는다 (다른 모듈의 클래스를 부르지 않는다).
 * 쿼리는 고정된 세 개(세기, 한 페이지, 인기 글)뿐이고 값은 모두 자리표시자로 넘긴다 (헌법 IV).
 */
@Repository
public class PostFeedRepository {

    private static final String COLUMNS = """
            select p.post_id, c.blog_id, b.name as blog_name, u.nickname, p.title, t.topic_name,
                   p.category_id, c.name as category_name, p.created_at, p.content,
                   (select count(*) from comment m where m.post_id = p.post_id) as comment_count,
                   (select count(*) from post_like l where l.post_id = p.post_id) as like_count
            """;

    private static final String FROM = """
            from post p
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            join users u on u.users_id = b.users_id
            join topic t on t.topic_id = p.topic_id
            """;

    /** 자리표시자 4개: 글 공개, 분류 공개, 주제 번호(없으면 null) 두 번. */
    private static final String OPEN_POSTS = """
            where p.visibility = ? and c.visibility = ? and u.deleted_at is null
              and (cast(? as bigint) is null or p.topic_id = cast(? as bigint))
            """;

    private static final String COUNT = "select count(*) " + FROM + OPEN_POSTS;

    /** 첫 화면 머리의 분량 (v0.5.1): 모든 공개 글의 블로그 번호와 본문. 매수는 글마다 올림하므로 본문을 읽어 센다. */
    private static final String OPEN_CONTENTS = "select c.blog_id, p.content " + FROM + OPEN_POSTS;

    /** 블로그 머리의 쓴 매수 (v0.5.1): 정해진 분류들의 글 본문. 자리표시자: 분류 번호 배열. */
    private static final String CONTENTS_IN_CATEGORIES = "select p.content from post p where p.category_id = any(?)";

    /** 위와 같고 공개 글만. 자리표시자: 분류 번호 배열, 글 공개. */
    private static final String PUBLIC_CONTENTS_IN_CATEGORIES = CONTENTS_IN_CATEGORIES + " and p.visibility = ?";

    /** 글쓰기 잔디 (013 US1): 정해진 분류들의 공개 글 가운데 since 이후의 작성 시각과 본문. 자리표시자: 분류 번호 배열, 글 공개, 시각. */
    private static final String PUBLIC_ACTIVITY_IN_CATEGORIES =
            "select p.created_at, p.content from post p where p.category_id = any(?) and p.visibility = ? and p.created_at >= ?";

    private static final String PAGE = COLUMNS + FROM + OPEN_POSTS + """
            order by p.created_at desc, p.post_id desc
            offset ? limit ?
            """;

    /** 인기 글: 기간(since ~ until)의 일별 조회수 합이 큰 순. 같으면 전체 조회수, 최신 순. 자리표시자는 기간 두 개가 맨 앞이다. */
    private static final String POPULAR = COLUMNS + FROM + """
            left join (select s.post_id, sum(s.views) as recent_views
                       from post_daily_stat s where s.stat_date between ? and ? group by s.post_id) r on r.post_id = p.post_id
            """ + OPEN_POSTS + """
            order by coalesce(r.recent_views, 0) desc, p.views desc, p.created_at desc, p.post_id desc
            limit ?
            """;

    private final JdbcTemplate jdbc;
    private final int readBatchSize;

    public PostFeedRepository(JdbcTemplate jdbc, ManuscriptProperties manuscript) {
        this.jdbc = jdbc;
        this.readBatchSize = manuscript.readBatchSize();
    }

    /** 공개 글 수. topicId가 null이면 모든 주제. */
    public long count(Long topicId) {
        Long count = jdbc.query(connection -> prepare(connection, COUNT, topicId), rs -> rs.next() ? rs.getLong(1) : 0L);
        return count == null ? 0 : count;
    }

    /**
     * 모든 공개 글을 하나씩 (블로그 번호, 본문)으로 넘긴다. 목록을 한꺼번에 쥐지 않도록 read-batch-size 줄씩 받아 온다
     * (PostgreSQL 드라이버는 트랜잭션 안에서만 나눠 받으므로 읽기 전용 트랜잭션에서 부른다).
     */
    public void forEachOpenPost(BiConsumer<Long, String> consumer) {
        jdbc.query(connection -> {
            PreparedStatement statement = prepare(connection, OPEN_CONTENTS, null);
            statement.setFetchSize(readBatchSize);
            return statement;
        }, (RowCallbackHandler) rs -> consumer.accept(rs.getLong(1), rs.getString(2)));
    }

    /**
     * 정해진 분류들의 글 본문을 하나씩 넘긴다 (블로그 머리의 쓴 매수). publicOnly면 공개 글만.
     * 어느 분류를 볼지는 PostVisibility.listScope가 정한다. forEachOpenPost와 같이 나눠 받는다.
     */
    public void forEachContentIn(Collection<Long> categoryIds, boolean publicOnly, Consumer<String> consumer) {
        jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    publicOnly ? PUBLIC_CONTENTS_IN_CATEGORIES : CONTENTS_IN_CATEGORIES);
            statement.setArray(1, connection.createArrayOf("bigint", categoryIds.toArray()));
            if (publicOnly) {
                statement.setString(2, Visibility.PUBLIC.value());
            }
            statement.setFetchSize(readBatchSize);
            return statement;
        }, (RowCallbackHandler) rs -> consumer.accept(rs.getString(1)));
    }

    /** 정해진 분류들의 공개 글 가운데 since 이후의 것을 하나씩 (작성 시각, 본문)으로 넘긴다 (013 US1 잔디). forEachContentIn과 같이 나눠 받는다. */
    public void forEachPublicPostSince(Collection<Long> categoryIds, Instant since, BiConsumer<Instant, String> consumer) {
        jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(PUBLIC_ACTIVITY_IN_CATEGORIES);
            statement.setArray(1, connection.createArrayOf("bigint", categoryIds.toArray()));
            statement.setString(2, Visibility.PUBLIC.value());
            statement.setTimestamp(3, Timestamp.from(since));
            statement.setFetchSize(readBatchSize);
            return statement;
        }, (RowCallbackHandler) rs -> consumer.accept(rs.getTimestamp(1).toInstant(), rs.getString(2)));
    }

    /** 최신순으로 offset부터 limit개. */
    public List<FeedItem> find(Long topicId, long offset, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(PAGE);
            int next = bindOpenPosts(statement, 1, topicId);
            statement.setLong(next, offset);
            statement.setInt(next + 1, limit);
            return statement;
        }, PostFeedRepository::toItem);
    }

    /** 인기 글 limit개. since~until은 기간의 첫날과 마지막 날(둘 다 포함). */
    public List<FeedItem> findPopular(Long topicId, LocalDate since, LocalDate until, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(POPULAR);
            statement.setObject(1, since);
            statement.setObject(2, until);
            int next = bindOpenPosts(statement, 3, topicId);
            statement.setInt(next, limit);
            return statement;
        }, PostFeedRepository::toItem);
    }

    private static PreparedStatement prepare(Connection connection, String sql, Long topicId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        bindOpenPosts(statement, 1, topicId);
        return statement;
    }

    /** OPEN_POSTS의 자리표시자 4개를 from 번째부터 채우고, 다음 자리 번호를 돌려준다. */
    private static int bindOpenPosts(PreparedStatement statement, int from, Long topicId) throws SQLException {
        statement.setString(from, Visibility.PUBLIC.value());
        statement.setString(from + 1, Visibility.PUBLIC.value());
        if (topicId == null) {
            statement.setNull(from + 2, Types.BIGINT);
            statement.setNull(from + 3, Types.BIGINT);
        } else {
            statement.setLong(from + 2, topicId);
            statement.setLong(from + 3, topicId);
        }
        return from + 4;
    }

    private static FeedItem toItem(ResultSet rs, int rowNum) throws SQLException {
        return new FeedItem(rs.getLong("post_id"), rs.getLong("blog_id"), rs.getString("blog_name"),
                rs.getString("nickname"), rs.getString("title"), rs.getString("topic_name"),
                rs.getLong("category_id"), rs.getString("category_name"), rs.getTimestamp("created_at").toInstant(),
                rs.getString("content"), rs.getLong("comment_count"), rs.getLong("like_count"));
    }

    /** 글 모음 한 줄. 본문은 미리보기를 만들 때만 쓴다. */
    public record FeedItem(long postId, long blogId, String blogName, String nickname, String title, String topicName,
            long categoryId, String categoryName, Instant createdAt, String content, long commentCount,
            long likeCount) {
    }
}
