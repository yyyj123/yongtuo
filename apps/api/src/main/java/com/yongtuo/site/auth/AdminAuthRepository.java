package com.yongtuo.site.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AdminAuthRepository {
    private final JdbcTemplate jdbc;

    AdminAuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<AdminAccount> findByUsername(String username) {
        return jdbc.query("""
                        SELECT id, username, password_hash, token_version
                        FROM admin_user
                        WHERE username = ?
                        """, (result, rowNumber) -> account(result), username)
                .stream().findFirst();
    }

    Optional<AdminAccount> findById(long id) {
        return jdbc.query("""
                        SELECT id, username, password_hash, token_version
                        FROM admin_user
                        WHERE id = ?
                        """, (result, rowNumber) -> account(result), id)
                .stream().findFirst();
    }

    Optional<AdminAccount> findByIdForUpdate(long id) {
        return jdbc.query("""
                        SELECT id, username, password_hash, token_version
                        FROM admin_user
                        WHERE id = ?
                        FOR UPDATE
                        """, (result, rowNumber) -> account(result), id)
                .stream().findFirst();
    }

    Integer tokenVersion(long id) {
        return jdbc.query("SELECT token_version FROM admin_user WHERE id = ?",
                        (result, rowNumber) -> result.getInt(1), id)
                .stream().findFirst().orElse(null);
    }

    void updateLastLogin(long id, Instant now) {
        jdbc.update("UPDATE admin_user SET last_login_at = ? WHERE id = ?", Timestamp.from(now), id);
    }

    void changePassword(long id, String passwordHash) {
        jdbc.update("""
                UPDATE admin_user
                SET password_hash = ?, token_version = token_version + 1
                WHERE id = ?
                """, passwordHash, id);
    }

    void insertSession(String sessionId, long adminUserId, String tokenHash,
                       int tokenVersion, Instant expiresAt) {
        jdbc.update("""
                INSERT INTO admin_refresh_session(
                  session_id, admin_user_id, token_hash, token_version, expires_at
                ) VALUES (?, ?, ?, ?, ?)
                """, sessionId, adminUserId, tokenHash, tokenVersion, Timestamp.from(expiresAt));
    }

    Optional<RefreshSession> findSessionForUpdate(String sessionId) {
        return jdbc.query("""
                        SELECT session_id, admin_user_id, token_hash, token_version,
                               expires_at, revoked_at
                        FROM admin_refresh_session
                        WHERE session_id = ?
                        FOR UPDATE
                        """, (result, rowNumber) -> new RefreshSession(
                        result.getString("session_id"), result.getLong("admin_user_id"),
                        result.getString("token_hash"), result.getInt("token_version"),
                        result.getTimestamp("expires_at").toInstant(),
                        result.getTimestamp("revoked_at") == null ? null
                                : result.getTimestamp("revoked_at").toInstant()), sessionId)
                .stream().findFirst();
    }

    void rotateSession(String oldSessionId, String replacementSessionId, Instant now) {
        jdbc.update("""
                UPDATE admin_refresh_session
                SET revoked_at = ?, replacement_session_id = ?
                WHERE session_id = ? AND revoked_at IS NULL
                """, Timestamp.from(now), replacementSessionId, oldSessionId);
    }

    void revokeSession(String sessionId, Instant now) {
        jdbc.update("""
                UPDATE admin_refresh_session
                SET revoked_at = COALESCE(revoked_at, ?)
                WHERE session_id = ?
                """, Timestamp.from(now), sessionId);
    }

    void revokeAllSessions(long adminUserId, Instant now) {
        jdbc.update("""
                UPDATE admin_refresh_session
                SET revoked_at = COALESCE(revoked_at, ?)
                WHERE admin_user_id = ?
                """, Timestamp.from(now), adminUserId);
    }

    private static AdminAccount account(java.sql.ResultSet result) throws java.sql.SQLException {
        return new AdminAccount(result.getLong("id"), result.getString("username"),
                result.getString("password_hash"), result.getInt("token_version"));
    }
}
