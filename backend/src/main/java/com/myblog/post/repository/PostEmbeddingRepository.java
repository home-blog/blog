package com.myblog.post.repository;

import static com.myblog.post.repository.PostFeedRepository.FROM;
import static com.myblog.post.repository.PostFeedRepository.OPEN_POSTS;
import static com.myblog.post.repository.PostFeedRepository.bindOpenPosts;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 글의 뜻 정보(임베딩) (specs/014 data-model 1, D-9). 벡터는 {@code real[]}로 두고, 비슷한 정도는 서버가 계산한다.
 * 공개 규칙은 글 모음과 같은 조각(PostFeedRepository.FROM, OPEN_POSTS)을 그대로 쓴다: 공개 분류의 공개 글, 탈퇴하지 않은 회원.
 */
@Repository
public class PostEmbeddingRepository {

    private final JdbcTemplate jdbc;

    public PostEmbeddingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 저장된 뜻 정보의 모델과 글 요약값(sha256). */
    public record Stored(String model, String contentHash) {
    }

    /** 임베딩할 글. changedAt은 읽은 때의 글 수정 시각(없으면 쓴 시각): 뜻 정보의 시각으로 저장한다. */
    public record Source(long postId, String title, String content, Instant changedAt) {
    }

    /** 비슷한 글 후보. */
    public record Candidate(long postId, long blogId, float[] embedding) {
    }

    /** 태그·주제로 고른 이웃. shared는 겹치는 태그 수. */
    public record Neighbor(long postId, long blogId, int shared, boolean sameTopic) {
    }

    public Optional<Stored> stored(long postId) {
        return jdbc.query("select model, content_hash from post_embedding where post_id = ?",
                rs -> rs.next() ? Optional.of(new Stored(rs.getString(1), rs.getString(2))) : Optional.<Stored>empty(), postId);
    }

    /** changedAt은 임베딩한 글의 수정 시각. 그 뒤에 고쳐지면 정리 작업(staleOpen)이 다시 만든다. */
    public void upsert(long postId, String model, String contentHash, float[] embedding, Instant changedAt) {
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into post_embedding (post_id, model, content_hash, embedding, updated_at) values (?, ?, ?, ?, ?)
                    on conflict (post_id) do update set model = excluded.model, content_hash = excluded.content_hash,
                        embedding = excluded.embedding, updated_at = excluded.updated_at
                    """);
            statement.setLong(1, postId);
            statement.setString(2, model);
            statement.setString(3, contentHash);
            statement.setArray(4, connection.createArrayOf("float4", box(embedding)));
            statement.setTimestamp(5, Timestamp.from(changedAt));
            return statement;
        });
    }

    /** 글이 고쳐졌지만 임베딩할 내용은 그대로일 때: 다시 보지 않게 시각만 그 글의 수정 시각으로 올린다. */
    public void touch(long postId, Instant changedAt) {
        jdbc.update("update post_embedding set updated_at = ? where post_id = ?", Timestamp.from(changedAt), postId);
    }

    /** 정해진 글 가운데 공개 글만 임베딩한다(비공개 글은 밖으로 보내지 않는다). */
    public List<Source> openSources(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "select p.post_id, p.title, p.content, coalesce(p.updated_at, p.created_at) " + FROM + OPEN_POSTS + " and p.post_id = any(?)");
            int next = bindOpenPosts(statement, 1, null);
            statement.setArray(next, connection.createArrayOf("bigint", postIds.toArray()));
            return statement;
        }, (rs, row) -> new Source(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getTimestamp(4).toInstant()));
    }

    /** 뜻 정보가 없거나, 모델이 다르거나, 그 뒤에 고쳐진 공개 글. 새 글부터 limit개 (정리 작업, D-11). */
    public List<Long> staleOpen(String model, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement("select p.post_id " + FROM
                    + "left join post_embedding e on e.post_id = p.post_id " + OPEN_POSTS + """
                       and (e.post_id is null or e.model <> ? or coalesce(p.updated_at, p.created_at) > e.updated_at)
                    order by p.created_at desc, p.post_id desc
                    limit ?
                    """);
            int next = bindOpenPosts(statement, 1, null);
            statement.setString(next, model);
            statement.setInt(next + 1, limit);
            return statement;
        }, (rs, row) -> rs.getLong(1));
    }

    /** 두 벌이 같이 돌 때 정리 작업은 한 벌만 (트랜잭션 안에서 부른다, 끝나면 풀린다). */
    public boolean tryBackfillLock() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select pg_try_advisory_xact_lock(hashtextextended('post-embedding-backfill', 0))", Boolean.class));
    }

    public Optional<float[]> vectorOf(long postId, String model) {
        return jdbc.query("select embedding from post_embedding where post_id = ? and model = ?",
                rs -> rs.next() ? Optional.of(unbox(rs.getArray(1))) : Optional.<float[]>empty(), postId, model);
    }

    /** 이 글을 뺀, 뜻 정보가 있는 공개 글. 새 글부터 limit개. */
    public List<Candidate> openCandidates(String model, long excludePostId, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement("select p.post_id, c.blog_id, e.embedding " + FROM
                    + "join post_embedding e on e.post_id = p.post_id " + OPEN_POSTS + """
                       and e.model = ? and p.post_id <> ?
                    order by p.created_at desc, p.post_id desc
                    limit ?
                    """);
            int next = bindOpenPosts(statement, 1, null);
            statement.setString(next, model);
            statement.setLong(next + 1, excludePostId);
            statement.setInt(next + 2, limit);
            return statement;
        }, (rs, row) -> new Candidate(rs.getLong(1), rs.getLong(2), unbox(rs.getArray(3))));
    }

    /** 태그·주제로 고른 이웃 (D-13): 겹치는 태그 수 → 같은 주제 → 최신 순. */
    public List<Neighbor> tagNeighbors(long postId, int limit) {
        return jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    select p.post_id, c.blog_id,
                           (select count(*) from post_tag a join post_tag b on b.tag_id = a.tag_id
                             where a.post_id = ? and b.post_id = p.post_id) as shared,
                           p.topic_id = (select s.topic_id from post s where s.post_id = ?) as same_topic
                    """ + FROM + OPEN_POSTS + """
                       and p.post_id <> ?
                    order by shared desc, same_topic desc, p.created_at desc, p.post_id desc
                    limit ?
                    """);
            statement.setLong(1, postId);
            statement.setLong(2, postId);
            int next = bindOpenPosts(statement, 3, null);
            statement.setLong(next, postId);
            statement.setInt(next + 1, limit);
            return statement;
        }, (rs, row) -> new Neighbor(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getBoolean(4)));
    }

    private static Float[] box(float[] vector) {
        Float[] boxed = new Float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            boxed[i] = vector[i];
        }
        return boxed;
    }

    private static float[] unbox(Array array) throws SQLException {
        Object raw = array.getArray();
        if (raw instanceof Float[] boxed) {
            float[] vector = new float[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                vector[i] = boxed[i];
            }
            return vector;
        }
        if (raw instanceof float[] primitive) {
            return primitive;
        }
        throw new SQLException("임베딩 칸의 모양이 다릅니다: " + raw.getClass());
    }
}
