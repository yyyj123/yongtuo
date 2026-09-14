package com.yongtuo.site.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {
    private static final String ISSUER = "yongtuo-api";
    private static final String AUDIENCE = "yongtuo-admin";

    private final AdminAuthRepository repository;
    private final AuthTokenProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final JwtEncoder accessEncoder;
    private final JwtEncoder refreshEncoder;
    private final JwtDecoder accessDecoder;
    private final JwtDecoder refreshDecoder;

    public AuthTokenService(AdminAuthRepository repository, AuthTokenProperties properties) {
        this.repository = repository;
        this.properties = properties;
        SecretKey accessKey = key(properties.accessSecret());
        SecretKey refreshKey = key(properties.refreshSecret());
        this.accessEncoder = NimbusJwtEncoder.withSecretKey(accessKey)
                .algorithm(MacAlgorithm.HS256).build();
        this.refreshEncoder = NimbusJwtEncoder.withSecretKey(refreshKey)
                .algorithm(MacAlgorithm.HS256).build();
        NimbusJwtDecoder access = NimbusJwtDecoder.withSecretKey(accessKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        access.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.accessDecoder = access;
        NimbusJwtDecoder refresh = NimbusJwtDecoder.withSecretKey(refreshKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        refresh.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.refreshDecoder = refresh;
    }

    @Transactional
    public AuthTokens issue(AdminAccount account) {
        Instant now = clock.instant();
        String sessionId = UUID.randomUUID().toString();
        Instant refreshExpiry = now.plus(properties.refreshTtl());
        String refreshToken = encode(refreshEncoder, account, sessionId, "refresh", now, refreshExpiry);
        repository.insertSession(sessionId, account.id(), hash(refreshToken),
                account.tokenVersion(), refreshExpiry);
        return tokens(account, sessionId, refreshToken, now);
    }

    @Transactional
    public AuthTokens rotate(String rawRefreshToken) {
        Jwt jwt = decodeRefresh(rawRefreshToken);
        String sessionId = jwt.getId();
        long adminId = subject(jwt);
        int tokenVersion = version(jwt);
        Instant now = clock.instant();
        RefreshSession session = repository.findSessionForUpdate(sessionId)
                .orElseThrow(AuthBusinessException::invalidToken);
        AdminAccount account = repository.findById(adminId)
                .orElseThrow(AuthBusinessException::invalidToken);
        if (session.adminUserId() != adminId || session.tokenVersion() != tokenVersion
                || account.tokenVersion() != tokenVersion || session.revokedAt() != null
                || !now.isBefore(session.expiresAt())
                || !MessageDigest.isEqual(session.tokenHash().getBytes(StandardCharsets.US_ASCII),
                        hash(rawRefreshToken).getBytes(StandardCharsets.US_ASCII))) {
            throw AuthBusinessException.invalidToken();
        }

        String replacementId = UUID.randomUUID().toString();
        Instant refreshExpiry = now.plus(properties.refreshTtl());
        String replacement = encode(refreshEncoder, account, replacementId,
                "refresh", now, refreshExpiry);
        repository.insertSession(replacementId, account.id(), hash(replacement),
                account.tokenVersion(), refreshExpiry);
        repository.rotateSession(sessionId, replacementId, now);
        return tokens(account, replacementId, replacement, now);
    }

    @Transactional
    public void revoke(String rawRefreshToken, long authenticatedAdminId) {
        Jwt jwt = decodeRefresh(rawRefreshToken);
        String sessionId = jwt.getId();
        RefreshSession session = repository.findSessionForUpdate(sessionId)
                .orElseThrow(AuthBusinessException::invalidToken);
        if (subject(jwt) != authenticatedAdminId || session.adminUserId() != authenticatedAdminId
                || !MessageDigest.isEqual(session.tokenHash().getBytes(StandardCharsets.US_ASCII),
                        hash(rawRefreshToken).getBytes(StandardCharsets.US_ASCII))) {
            throw AuthBusinessException.invalidToken();
        }
        repository.revokeSession(sessionId, clock.instant());
    }

    public Jwt decodeAccess(String token) throws JwtException {
        Jwt jwt = accessDecoder.decode(token);
        requireToken(jwt, "access");
        long adminId = subject(jwt);
        Integer currentVersion = repository.tokenVersion(adminId);
        if (currentVersion == null || currentVersion != version(jwt)) {
            throw new BadJwtException("Access token is no longer valid");
        }
        return jwt;
    }

    long authenticatedAdminId(Jwt jwt) {
        return subject(jwt);
    }

    private Jwt decodeRefresh(String token) {
        try {
            Jwt jwt = refreshDecoder.decode(token);
            requireToken(jwt, "refresh");
            return jwt;
        } catch (JwtException | IllegalArgumentException exception) {
            throw AuthBusinessException.invalidToken();
        }
    }

    private AuthTokens tokens(AdminAccount account, String sessionId,
                              String refreshToken, Instant now) {
        String accessToken = encode(accessEncoder, account, sessionId, "access",
                now, now.plus(properties.accessTtl()));
        return new AuthTokens(accessToken, refreshToken, "Bearer", properties.accessTtl().toSeconds());
    }

    private static String encode(JwtEncoder encoder, AdminAccount account, String sessionId,
                                 String type, Instant issuedAt, Instant expiresAt) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(Long.toString(account.id()))
                .audience(List.of(AUDIENCE))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(type.equals("refresh") ? sessionId : UUID.randomUUID().toString())
                .claim("sid", sessionId)
                .claim("typ", type)
                .claim("ver", account.tokenVersion())
                .claim("usr", account.username())
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static void requireToken(Jwt jwt, String type) {
        if (jwt.getExpiresAt() == null || !type.equals(jwt.getClaimAsString("typ")) || jwt.getAudience() == null
                || !jwt.getAudience().contains(AUDIENCE)) {
            throw new BadJwtException("Invalid token type or audience");
        }
    }

    private static long subject(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BadJwtException("Invalid token subject");
        }
    }

    private static int version(Jwt jwt) {
        Object value = jwt.getClaim("ver");
        if (!(value instanceof Integer || value instanceof Long)
                || ((Number) value).longValue() < 0 || ((Number) value).longValue() > Integer.MAX_VALUE) {
            throw new BadJwtException("Invalid token version");
        }
        return ((Number) value).intValue();
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
