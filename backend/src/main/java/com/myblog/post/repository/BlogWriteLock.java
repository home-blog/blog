package com.myblog.post.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

/**
 * 한 블로그의 연재·임시 저장 쓰기를 차례로 세운다 (specs/013, PR #72 리뷰). 한도를 센 뒤 넣는 사이에 다른 요청이 끼면
 * 한도를 넘거나 화 순서가 겹친다. PostgreSQL 트랜잭션 잠금(pg_advisory_xact_lock)이라 부른 트랜잭션이 끝나면 풀린다.
 * 이미지 올리기의 회원 잠금과 같은 방법이다. 블로그 표는 블로그 모듈의 것이라 줄을 잠그지 않고 이름으로 잠근다.
 */
@Repository
public class BlogWriteLock {

    private static final String LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;

    private final JdbcTemplate jdbc;

    public BlogWriteLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 이 블로그의 연재 쓰기 (만들기, 글 넣기·빼기, 순서). 트랜잭션 안에서 부른다. */
    public void series(Long blogId) {
        jdbc.query(LOCK_SQL, IGNORE, "series:" + blogId);
    }

    /** 이 블로그의 임시 저장 글 만들기 (개수 한도). 트랜잭션 안에서 부른다. */
    public void drafts(Long blogId) {
        jdbc.query(LOCK_SQL, IGNORE, "draft:" + blogId);
    }
}
