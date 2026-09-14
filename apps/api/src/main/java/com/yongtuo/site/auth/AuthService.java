package com.yongtuo.site.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AdminAuthRepository repository;
    private final AuthTokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock = Clock.systemUTC();
    private final String dummyPasswordHash;
    private final LoginAttemptLimiter limiter;
    private final LoginAuditService audit;

    public AuthService(AdminAuthRepository repository, AuthTokenService tokens,
                       PasswordEncoder passwordEncoder, LoginAttemptLimiter limiter, LoginAuditService audit) {
        this.repository = repository;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.limiter = limiter;
        this.audit = audit;
    }

    @Transactional(noRollbackFor = AuthBusinessException.class)
    public AuthTokens login(LoginRequest request, String ip, String userAgent) {
        AdminAccount account = repository.findByUsername(request.username().strip()).orElse(null);
        String accountKey = account == null ? "unknown:" + request.username().strip().toLowerCase(java.util.Locale.ROOT)
                : "id:" + account.id();
        if (!limiter.acquire(accountKey, ip)) {
            audit.record(account, request.username(), ip, userAgent, "LOGIN_THROTTLED");
            throw AuthBusinessException.throttled();
        }
        String hash = account == null ? dummyPasswordHash : account.passwordHash();
        if (!passwordEncoder.matches(request.password(), hash) || account == null) {
            audit.record(account, request.username(), ip, userAgent, "LOGIN_FAILED");
            throw AuthBusinessException.loginFailed();
        }
        Instant now = clock.instant();
        repository.updateLastLogin(account.id(), now);
        AuthTokens result = tokens.issue(account);
        audit.record(account, account.username(), ip, userAgent, null);
        limiter.succeeded(accountKey, ip);
        return result;
    }

    public AuthTokens refresh(RefreshTokenRequest request) {
        return tokens.rotate(request.refreshToken());
    }

    public void logout(RefreshTokenRequest request, Jwt jwt) {
        tokens.revoke(request.refreshToken(), tokens.authenticatedAdminId(jwt));
    }

    @Transactional(readOnly = true)
    public AuthMe me(Jwt jwt) {
        AdminAccount account = repository.findById(tokens.authenticatedAdminId(jwt))
                .orElseThrow(AuthBusinessException::unauthorized);
        return new AuthMe(account.id(), account.username());
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, Jwt jwt) {
        long adminId = tokens.authenticatedAdminId(jwt);
        AdminAccount account = repository.findByIdForUpdate(adminId)
                .orElseThrow(AuthBusinessException::unauthorized);
        if (!passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            throw AuthBusinessException.invalidCurrentPassword();
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw AuthBusinessException.invalidCurrentPassword();
        }
        Instant now = clock.instant();
        repository.changePassword(adminId, passwordEncoder.encode(request.newPassword()));
        repository.revokeAllSessions(adminId, now);
    }
}
