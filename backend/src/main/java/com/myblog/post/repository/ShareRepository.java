package com.myblog.post.repository;

import com.myblog.common.Visibility;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 로봇이 읽는 주소(RSS, 사이트 지도)에 쓰는 읽기 전용 쿼리 (specs/007 US5·US6).
 * 첫 화면 글 모음(PostFeedRepository)과 같은 공개 조건: <b>공개 분류의 공개 글</b>, 탈퇴한 회원의 블로그는 뺀다.
 * 다른 모듈의 표(blog, category, users)는 SQL로만 읽는다. 값은 모두 자리표시자로 넘긴다 (헌법 IV).
 */
@Repository
public class ShareRepository {

    private static final String OPEN_POST_FROM = """
            from post p
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            join users u on u.users_id = b.users_id
            join topic t on t.topic_id = p.topic_id
            where p.visibility = ? and c.visibility = ? and u.deleted_at is null
            """;

    /** 자리표시자: 공개 두 번, 블로그 번호(없으면 null) 두 번, 개수. */
    private static final String LATEST = """
            select p.post_id, p.title, p.content, p.created_at, coalesce(p.updated_at, p.created_at) as modified_at,
                   t.topic_name, b.name as blog_name
            """ + OPEN_POST_FROM + """
              and (cast(? as bigint) is null or c.blog_id = cast(? as bigint))
            order by p.created_at desc, p.post_id desc
            limit ?
            """;

    /** 사이트 지도용: 본문 없이 번호와 고친 시각만. 자리표시자: 공개 두 번, 개수. */
    private static final String LATEST_STAMPS = """
            select p.post_id, coalesce(p.updated_at, p.created_at) as modified_at
            """ + OPEN_POST_FROM + """
            order by p.created_at desc, p.post_id desc
            limit ?
            """;

    private static final String OPEN_BLOG = """
            select b.blog_id, b.name, b.intro
            from blog b join users u on u.users_id = b.users_id
            where b.blog_id = ? and u.deleted_at is null
            """;

    private static final String OPEN_BLOG_IDS = """
            select b.blog_id
            from blog b join users u on u.users_id = b.users_id
            where u.deleted_at is null
            order by b.blog_id
            """;

    private final JdbcTemplate jdbc;

    public ShareRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 최신 공개 글 limit개. blogId가 null이면 모든 블로그. */
    public List<SharedPost> latest(Long blogId, int limit) {
        return jdbc.query(connection -> {
            var statement = connection.prepareStatement(LATEST);
            statement.setString(1, Visibility.PUBLIC.value());
            statement.setString(2, Visibility.PUBLIC.value());
            if (blogId == null) {
                statement.setNull(3, Types.BIGINT);
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(3, blogId);
                statement.setLong(4, blogId);
            }
            statement.setInt(5, limit);
            return statement;
        }, ShareRepository::toPost);
    }

    /** 사이트 지도에 넣을 최신 공개 글의 번호와 고친 시각. */
    public List<PostStamp> latestStamps(int limit) {
        return jdbc.query(LATEST_STAMPS, (rs, rowNum) -> new PostStamp(rs.getLong("post_id"),
                instant(rs.getTimestamp("modified_at"))), Visibility.PUBLIC.value(), Visibility.PUBLIC.value(), limit);
    }

    /** 주인이 탈퇴하지 않은 블로그. */
    public Optional<SharedBlog> openBlog(long blogId) {
        return jdbc.query(OPEN_BLOG, (rs, rowNum) -> new SharedBlog(rs.getLong("blog_id"), rs.getString("name"),
                rs.getString("intro")), blogId).stream().findFirst();
    }

    /** 주인이 탈퇴하지 않은 블로그 번호 모두 (사이트 지도). */
    public List<Long> openBlogIds() {
        return jdbc.queryForList(OPEN_BLOG_IDS, Long.class);
    }

    private static SharedPost toPost(ResultSet rs, int rowNum) throws SQLException {
        return new SharedPost(rs.getLong("post_id"), rs.getString("title"), rs.getString("content"),
                instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("modified_at")),
                rs.getString("topic_name"), rs.getString("blog_name"));
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    /** 공개 글 한 줄. modifiedAt은 고친 적이 없으면 쓴 시각. */
    public record SharedPost(long postId, String title, String content, Instant createdAt, Instant modifiedAt,
            String topicName, String blogName) {
    }

    public record SharedBlog(long blogId, String name, String intro) {
    }

    public record PostStamp(long postId, Instant modifiedAt) {
    }
}
