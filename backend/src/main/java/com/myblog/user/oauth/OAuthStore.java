package com.myblog.user.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** oauth_client·oauth_grant·oauth_access_token 표 (specs/023 data-model). 고정 쿼리에 값만 넣는다. */
@Repository
class OAuthStore {

    private static final RowMapper<Client> CLIENT = (rs, rowNum) -> new Client(rs.getString("client_id"),
            rs.getString("client_name"), List.of(rs.getString("redirect_uris").split("\n")),
            instant(rs, "created_at"));
    private static final RowMapper<Grant> GRANT = (rs, rowNum) -> new Grant(rs.getLong("oauth_grant_id"),
            rs.getLong("users_id"), rs.getString("client_id"), rs.getString("client_name"), rs.getString("scope"),
            instant(rs, "refresh_expires_at"), instant(rs, "created_at"), instant(rs, "last_used_at"));
    private static final String GRANT_COLUMNS = "g.oauth_grant_id, g.users_id, g.client_id, c.client_name, g.scope,"
            + " g.refresh_expires_at, g.created_at, g.last_used_at"
            + " from oauth_grant g join oauth_client c on c.client_id = g.client_id";

    private final JdbcTemplate jdbc;

    OAuthStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Client(String clientId, String name, List<String> redirectUris, Instant createdAt) {
    }

    record Grant(long grantId, long memberId, String clientId, String clientName, String scope,
            Instant refreshExpiresAt, Instant createdAt, Instant lastUsedAt) {
    }

    /** 출입증으로 찾은 연결 (기한은 부르는 쪽이 본다). */
    record AccessLookup(long grantId, long memberId, String scope, Instant expiresAt, Instant lastUsedAt) {
    }

    void insertClient(Client client) {
        jdbc.update("insert into oauth_client (client_id, client_name, redirect_uris, created_at, last_used_at)"
                        + " values (?, ?, ?, ?, ?)", client.clientId(), client.name(),
                String.join("\n", client.redirectUris()), Timestamp.from(client.createdAt()),
                Timestamp.from(client.createdAt()));
    }

    /** 앱이 인가·토큰을 부를 때: 마지막 사용 시각 (1분보다 자주 쓰지 않는다). 연결 없이 오래된 앱만 지운다. */
    void touchClient(String clientId, Instant now) {
        jdbc.update("update oauth_client set last_used_at = ? where client_id = ? and last_used_at < ?",
                Timestamp.from(now), clientId, Timestamp.from(now.minusSeconds(60)));
    }

    Optional<Client> client(String clientId) {
        return jdbc.query("select client_id, client_name, redirect_uris, created_at from oauth_client where client_id = ?",
                CLIENT, clientId).stream().findFirst();
    }

    long insertGrant(long memberId, String clientId, String scope, String refreshHash, Instant refreshExpiresAt,
            Instant now) {
        Long id = jdbc.queryForObject("insert into oauth_grant (users_id, client_id, scope, refresh_hash,"
                        + " refresh_expires_at, created_at, last_used_at) values (?, ?, ?, ?, ?, ?, ?) returning oauth_grant_id",
                Long.class, memberId, clientId, scope, refreshHash, Timestamp.from(refreshExpiresAt),
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    void insertAccessToken(String tokenHash, long grantId, Instant expiresAt) {
        jdbc.update("insert into oauth_access_token (token_hash, oauth_grant_id, expires_at) values (?, ?, ?)",
                tokenHash, grantId, Timestamp.from(expiresAt));
    }

    Optional<AccessLookup> findByAccessToken(String tokenHash) {
        return jdbc.query("select g.oauth_grant_id, g.users_id, g.scope, t.expires_at, g.last_used_at"
                        + " from oauth_access_token t join oauth_grant g on g.oauth_grant_id = t.oauth_grant_id"
                        + " where t.token_hash = ?",
                (rs, rowNum) -> new AccessLookup(rs.getLong("oauth_grant_id"), rs.getLong("users_id"),
                        rs.getString("scope"), instant(rs, "expires_at"), instant(rs, "last_used_at")),
                tokenHash).stream().findFirst();
    }

    /**
     * 갱신 토큰 돌리기: 지금 갱신 토큰이 oldHash인 연결만 한 줄로 바꾼다(같은 토큰으로 겹쳐 와도 하나만 통과).
     * 바뀐 연결을 돌려준다.
     */
    Optional<Grant> rotateRefresh(String oldHash, String clientId, String newHash, Instant newExpiresAt, Instant now) {
        List<Long> ids = jdbc.queryForList("update oauth_grant set previous_refresh_hash = refresh_hash,"
                        + " refresh_hash = ?, refresh_expires_at = ?, last_used_at = ?, refresh_rotated_at = ?"
                        + " where refresh_hash = ? and client_id = ? and refresh_expires_at > ? returning oauth_grant_id",
                Long.class, newHash, Timestamp.from(newExpiresAt), Timestamp.from(now), Timestamp.from(now), oldHash,
                clientId, Timestamp.from(now));
        return ids.stream().findFirst().flatMap(this::grant);
    }

    /** 이미 바뀐(옛) 갱신 토큰이 다시 왔을 때 그 연결과, 갱신 토큰을 바꾼 시각·앱. */
    record PreviousRefresh(long grantId, String clientId, Instant rotatedAt) {
    }

    Optional<PreviousRefresh> grantByPreviousRefresh(String hash) {
        return jdbc.query("select oauth_grant_id, client_id, refresh_rotated_at from oauth_grant"
                        + " where previous_refresh_hash = ?",
                (rs, rowNum) -> new PreviousRefresh(rs.getLong("oauth_grant_id"), rs.getString("client_id"),
                        instant(rs, "refresh_rotated_at")), hash).stream().findFirst();
    }

    Optional<Long> grantByRefresh(String hash) {
        return jdbc.queryForList("select oauth_grant_id from oauth_grant where refresh_hash = ?", Long.class, hash)
                .stream().findFirst();
    }

    Optional<Grant> grant(long grantId) {
        return jdbc.query("select " + GRANT_COLUMNS + " where g.oauth_grant_id = ?", GRANT, grantId).stream()
                .findFirst();
    }

    List<Grant> grantsOf(long memberId) {
        return jdbc.query("select " + GRANT_COLUMNS + " where g.users_id = ?"
                + " order by g.created_at desc, g.oauth_grant_id desc", GRANT, memberId);
    }

    /** 가장 오래 쓰지 않은 연결부터 count개를 지운다 (S2-2). */
    int deleteLeastRecentlyUsed(long memberId, int count) {
        if (count <= 0) {
            return 0;
        }
        return jdbc.update("delete from oauth_grant where oauth_grant_id in (select oauth_grant_id from oauth_grant"
                + " where users_id = ? order by coalesce(last_used_at, created_at), oauth_grant_id limit ?)",
                memberId, count);
    }

    long countGrants(long memberId) {
        Long count = jdbc.queryForObject("select count(*) from oauth_grant where users_id = ?", Long.class, memberId);
        return count == null ? 0 : count;
    }

    int deleteGrant(long grantId) {
        return jdbc.update("delete from oauth_grant where oauth_grant_id = ?", grantId);
    }

    int deleteGrant(long grantId, long memberId) {
        return jdbc.update("delete from oauth_grant where oauth_grant_id = ? and users_id = ?", grantId, memberId);
    }

    int deleteGrantsOf(long memberId) {
        return jdbc.update("delete from oauth_grant where users_id = ?", memberId);
    }

    /** 연결을 모두 끊은 시각을 남긴다: 그 전에 받은 허락 코드는 쓸 수 없다. */
    void markRevoked(long memberId, Instant now) {
        jdbc.update("update users set oauth_revoked_at = ? where users_id = ?", Timestamp.from(now), memberId);
    }

    Optional<Instant> revokedAt(long memberId) {
        List<Timestamp> values = jdbc.queryForList("select oauth_revoked_at from users where users_id = ?",
                Timestamp.class, memberId);
        return values.isEmpty() || values.get(0) == null ? Optional.empty() : Optional.of(values.get(0).toInstant());
    }

    int deleteAccessToken(String tokenHash) {
        return jdbc.update("delete from oauth_access_token where token_hash = ?", tokenHash);
    }

    /** 마지막 사용: 처음이거나 since보다 오래됐을 때만 고친다(요청마다 쓰지 않게). */
    void touch(long grantId, Instant now, Instant since) {
        jdbc.update("update oauth_grant set last_used_at = ? where oauth_grant_id = ?"
                + " and (last_used_at is null or last_used_at < ?)", Timestamp.from(now), grantId, Timestamp.from(since));
    }

    int deleteExpiredAccessTokens(Instant now) {
        return jdbc.update("delete from oauth_access_token where expires_at <= ?", Timestamp.from(now));
    }

    int deleteExpiredGrants(Instant now) {
        return jdbc.update("delete from oauth_grant where refresh_expires_at <= ?", Timestamp.from(now));
    }

    int deleteUnusedClients(Instant before) {
        return jdbc.update("delete from oauth_client c where c.last_used_at < ?"
                + " and not exists (select 1 from oauth_grant g where g.client_id = c.client_id)", Timestamp.from(before));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
