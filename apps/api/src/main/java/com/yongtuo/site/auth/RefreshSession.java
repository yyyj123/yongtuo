package com.yongtuo.site.auth;

import java.time.Instant;

record RefreshSession(String sessionId, long adminUserId, String tokenHash,
                      int tokenVersion, Instant expiresAt, Instant revokedAt) {
}
