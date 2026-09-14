package com.yongtuo.site.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoginProtectionIntegrationTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private static final String PASSWORD = "Synthetic-login-password!";

    @BeforeEach void seed() {
        jdbc.update("DELETE FROM admin_login_log");
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user(username, password_hash) VALUES (?, ?)",
                "synthetic-admin", new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @Test void accountLimitPersistsFailuresAndIgnoresIpRotationAndUsernameCase() throws Exception {
        for (int i = 0; i < 5; i++) login("synthetic-admin", "wrong", "192.0.2." + i)
                .andExpect(status().isUnauthorized());
        login("SYNTHETIC-ADMIN", PASSWORD, "192.0.2.100")
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value(10005));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_login_log", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT failure_reason FROM admin_login_log", String.class))
                .containsOnly("LOGIN_FAILED", "LOGIN_THROTTLED");
    }

    @Test void ipLimitCannotBeBypassedWithDifferentAccountsOrForwardedHeaders() throws Exception {
        for (int i = 0; i < 5; i++) login("unknown-" + i, "wrong", "198.51.100.10")
                .andExpect(status().isUnauthorized());
        login("synthetic-admin", PASSWORD, "198.51.100.10")
                .andExpect(status().isTooManyRequests());
    }

    @Test void successfulLoginResetsCountersAndLogsContainNoCredentials() throws Exception {
        for (int i = 0; i < 4; i++) login("synthetic-admin", "wrong", "203.0.113.10")
                .andExpect(status().isUnauthorized());
        login("synthetic-admin", PASSWORD, "203.0.113.10").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) login("synthetic-admin", "wrong", "203.0.113.10")
                .andExpect(status().isUnauthorized());
        login("synthetic-admin", PASSWORD, "203.0.113.10").andExpect(status().isOk());
        var rows = jdbc.queryForList("SELECT * FROM admin_login_log");
        assertThat(rows).hasSize(10);
        assertThat(rows.toString()).doesNotContain(PASSWORD, "refreshToken", "accessToken", "password_hash");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_login_log WHERE login_success=1",
                Integer.class)).isEqualTo(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("ip_address")).isEqualTo("203.0.113.10");
            assertThat(row.get("user_agent")).isEqualTo("Synthetic-Test/1.0");
        });
    }

    private ResultActions login(String user, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/login").contentType("application/json")
                .header("User-Agent", "Synthetic-Test/1.0")
                .header("X-Forwarded-For", java.util.UUID.randomUUID().toString())
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"));
    }
}
