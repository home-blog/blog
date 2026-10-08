package com.myblog.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 005 테스트가 쓰는 도우미: 5초 간격을 기다리지 않게 회원의 댓글 시각을 뒤로 돌리고, 끝에서 회원의 데이터를 지운다.
 * 회원 줄은 댓글·좋아요·신고가 가리키므로 그것들을 먼저 지운다 (외래 키에 연쇄 삭제 없음).
 */
@Component
public class TestComments {

    private final JdbcTemplate jdbc;

    public TestComments(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 이 회원의 댓글을 모두 1분 전에 쓴 것으로 바꾼다 (다음 댓글이 간격에 걸리지 않게). */
    public void rewind(Long memberId) {
        jdbc.update("update comment set created_at = created_at - interval '1 minute' where users_id = ?", memberId);
    }

    /** 회원과 그 블로그·글, 그 회원이 쓴 것과 그 회원의 글에 달린 것을 모두 지운다. */
    public void deleteMember(TestMembers.Member member) {
        String postsOfBlog = "select post_id from post where category_id in (select category_id from category where blog_id = ?)";
        jdbc.update("delete from comment where users_id = ? or post_id in (" + postsOfBlog + ")", member.id(), member.blogId());
        jdbc.update("delete from post_report where users_id = ? or post_id in (" + postsOfBlog + ")", member.id(), member.blogId());
        jdbc.update("delete from post_like where users_id = ? or post_id in (" + postsOfBlog + ")", member.id(), member.blogId());
        jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", member.blogId());
        jdbc.update("delete from category where blog_id = ?", member.blogId());
        jdbc.update("delete from blog where blog_id = ?", member.blogId());
        jdbc.update("delete from users where users_id = ?", member.id());
    }
}
