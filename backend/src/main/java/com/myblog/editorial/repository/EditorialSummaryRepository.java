package com.myblog.editorial.repository;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 운영 현황 숫자 (specs/018 US3, D-6). 관리자만 보므로 요청마다 센다. 여러 모듈의 표를 SQL로만 읽고 쓰지 않는다
 * (글 검색과 같은 방식). 블로그 글은 탈퇴하지 않은 회원의 공개 분류의 공개 글만 센다(첫 화면과 같은 기준).
 */
@Repository
public class EditorialSummaryRepository {

    private static final String PUBLIC_POSTS = """
            select count(*) from post p
            join category c on c.category_id = p.category_id
            join blog b on b.blog_id = c.blog_id
            join users u on u.users_id = b.users_id
            where p.visibility = 'public' and c.visibility = 'public' and u.deleted_at is null
            """;

    private final JdbcTemplate jdbc;

    public EditorialSummaryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long members() {
        return count("select count(*) from users where deleted_at is null");
    }

    public long membersSince(Instant from) {
        return count("select count(*) from users where deleted_at is null and created_at >= ?", Timestamp.from(from));
    }

    public long publicPosts() {
        return count(PUBLIC_POSTS);
    }

    public long publicPostsSince(Instant from) {
        return count(PUBLIC_POSTS + " and p.created_at >= ?", Timestamp.from(from));
    }

    public long boardPosts() {
        return count("select count(*) from board_post");
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
