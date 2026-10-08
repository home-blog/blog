package com.myblog.comment.service;

import com.myblog.comment.CommentStatsQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommentStatsQuery를 댓글 표로 채운다 (specs/006 T040, research R-1). 기간의 처음·끝은 순간으로 비교하고,
 * 날짜는 DB가 작성 시각을 받은 시간대로 바꿔 묶는다. 시간대 이름도 파라미터로 넘긴다 (글자를 이어 붙이지 않음).
 */
@Component
@Transactional(readOnly = true)
public class CommentStatsQueryAdapter implements CommentStatsQuery {

    private static final String DAILY_COUNTS_SQL = "select cast(created_at at time zone :zone as date) as day, count(*) as cnt"
            + " from comment where post_id in (:postIds) and created_at >= :from and created_at < :to group by 1";

    private final NamedParameterJdbcTemplate jdbc;

    public CommentStatsQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<LocalDate, Long> dailyCounts(Collection<Long> postIds, Instant from, Instant to, ZoneId zone) {
        Map<LocalDate, Long> counts = new HashMap<>();
        if (postIds.isEmpty()) {
            return counts;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("zone", zone.getId())
                .addValue("postIds", postIds)
                .addValue("from", from.atOffset(ZoneOffset.UTC))
                .addValue("to", to.atOffset(ZoneOffset.UTC));
        jdbc.query(DAILY_COUNTS_SQL, params, rs -> {
            counts.put(rs.getObject("day", LocalDate.class), rs.getLong("cnt"));
        });
        return counts;
    }
}
