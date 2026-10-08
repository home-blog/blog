package com.myblog.support;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 006 테스트가 쓰는 도우미: 일별 통계 줄을 표에 바로 넣고(쌓인 숫자 준비), 끝에서 회원 블로그의 통계 줄을 지운다.
 * 통계 표가 블로그·글을 가리키므로(연쇄 삭제 없음) 블로그·글을 지우기 전에 부른다.
 */
@Component
public class TestStats {

    private final JdbcTemplate jdbc;

    public TestStats(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void addBlogDay(Long blogId, LocalDate date, int views, int visitors) {
        jdbc.update("insert into blog_daily_stat (blog_id, stat_date, views, visitors) values (?, ?, ?, ?)",
                blogId, date, views, visitors);
    }

    public void addPostDay(Long postId, LocalDate date, int views) {
        jdbc.update("insert into post_daily_stat (post_id, stat_date, views) values (?, ?, ?)", postId, date, views);
    }

    public long blogRows(Long blogId) {
        return jdbc.queryForObject("select count(*) from blog_daily_stat where blog_id = ?", Long.class, blogId);
    }

    public long postRows(Long postId) {
        return jdbc.queryForObject("select count(*) from post_daily_stat where post_id = ?", Long.class, postId);
    }

    /** 이 블로그와 그 글의 통계 줄을 모두 지운다. */
    public void deleteFor(Long blogId) {
        jdbc.update("delete from post_daily_stat where post_id in (select post_id from post where category_id in"
                + " (select category_id from category where blog_id = ?))", blogId);
        jdbc.update("delete from blog_daily_stat where blog_id = ?", blogId);
    }
}
