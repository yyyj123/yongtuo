package com.yongtuo.site.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {
    private static final String USERNAME = "synthetic-admin";
    private static final String PASSWORD = "Initial-pass-2026!";
    private static final String NEW_PASSWORD = "Changed-pass-2026!";

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("security.jwt.access-secret",
                () -> "synthetic-test-access-secret-at-least-thirty-two-bytes");
        registry.add("security.jwt.refresh-secret",
                () -> "synthetic-test-refresh-secret-at-least-thirty-two-bytes");
        registry.add("security.jwt.access-ttl", () -> "PT15M");
        registry.add("security.jwt.refresh-ttl", () -> "P30D");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentRefreshSucceedsExactlyOnceAndStoresOnlyDigests() throws Exception {
        Tokens initial = login(PASSWORD);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> attempt = () -> {
                start.await();
                return mvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(APPLICATION_JSON).content(refreshJson(initial.refreshToken())))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(java.util.List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
        }
        assertThat(jdbc.queryForList("SELECT token_hash FROM admin_refresh_session", String.class))
                .hasSize(2).allMatch(hash -> hash.matches("[0-9a-f]{64}"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_refresh_session WHERE revoked_at IS NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void accessAndRefreshCannotBeInterchanged() throws Exception {
        Tokens initial = login(PASSWORD);
        mvc.perform(get("/api/v1/admin/auth/me").header(AUTHORIZATION, bearer(initial.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/auth/refresh").contentType(APPLICATION_JSON)
                        .content(refreshJson(initial.accessToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongSignatureIssuerAudienceTypeAndExpiredTokens() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM admin_user", Long.class);
        for (String fault : java.util.List.of("secret", "issuer", "audience", "type", "expiry", "version", "missing-expiry")) {
            String key = fault.equals("secret") ? "synthetic-wrong-secret-at-least-thirty-two-bytes"
                    : "synthetic-test-access-secret-at-least-thirty-two-bytes";
            var encoder = org.springframework.security.oauth2.jwt.NimbusJwtEncoder.withSecretKey(
                    new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
                    .algorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
            var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                    .issuer(fault.equals("issuer") ? "wrong" : "yongtuo-api")
                    .subject(Long.toString(id))
                    .audience(java.util.List.of(fault.equals("audience") ? "wrong" : "yongtuo-admin"))
                    .issuedAt(java.time.Instant.now().minusSeconds(300))
                    .claim("typ", fault.equals("type") ? "refresh" : "access")
                    .claim("ver", fault.equals("version") ? (Object) 0.5 : (Object) 0);
            if (!fault.equals("missing-expiry")) claims.expiresAt(java.time.Instant.now()
                    .plusSeconds(fault.equals("expiry") ? -120 : 300));
            String token = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                    org.springframework.security.oauth2.jwt.JwsHeader.with(
                            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            mvc.perform(get("/api/v1/admin/auth/me").header(AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @BeforeEach
    void seedAdmin() {
        jdbc.update("DELETE FROM admin_login_log");
        jdbc.update("DELETE FROM admin_operation_log");
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("""
                INSERT INTO admin_user(username, password_hash, token_version)
                VALUES (?, ?, 0)
                """, USERNAME, new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @Test
    void validLoginIssuesTokensAndAccessesAdminResources() throws Exception {
        Tokens tokens = login(PASSWORD);

        assertThat(tokens.accessToken()).isNotBlank().isNotEqualTo(tokens.refreshToken());
        assertThat(tokens.refreshToken()).isNotBlank();
        mvc.perform(get("/api/v1/admin/auth/me")
                        .header(AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME));
        mvc.perform(get("/api/v1/admin/categories/tree")
                        .header(AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPasswordAndMissingTokenAreRejectedWithoutLeakingTokens() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson("incorrect-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data").doesNotExist());

        mvc.perform(get("/api/v1/admin/categories/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10003));
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesLatestRefreshToken() throws Exception {
        Tokens initial = login(PASSWORD);

        MvcResult rotatedResult = mvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson(initial.refreshToken())))
                .andExpect(status().isOk())
                .andReturn();
        Tokens rotated = tokens(rotatedResult);
        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());

        mvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson(initial.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        mvc.perform(post("/api/v1/admin/auth/logout")
                        .header(AUTHORIZATION, bearer(rotated.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson(rotated.refreshToken())))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson(rotated.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void passwordChangeInvalidatesOldTokensAndRequiresNewPassword() throws Exception {
        Tokens initial = login(PASSWORD);

        mvc.perform(put("/api/v1/admin/auth/password")
                        .header(AUTHORIZATION, bearer(initial.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/auth/me")
                        .header(AUTHORIZATION, bearer(initial.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10003));
        mvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson(initial.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
        mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10001));

        Tokens current = login(NEW_PASSWORD);
        assertThat(current.accessToken()).isNotBlank();
    }

    private Tokens login(String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson(password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900))
                .andReturn();
        return tokens(result);
    }

    private static Tokens tokens(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.data.accessToken"),
                JsonPath.read(body, "$.data.refreshToken"));
    }

    private static String loginJson(String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(USERNAME, password);
    }

    private static String refreshJson(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}
