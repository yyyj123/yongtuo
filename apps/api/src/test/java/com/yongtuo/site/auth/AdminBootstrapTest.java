package com.yongtuo.site.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class AdminBootstrapTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");
    @BeforeAll static void migrate() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load().migrate();
    }
    @Test void initializesOnceAndNeverResetsAnExistingAccount() throws Exception {
        try (var connection = MYSQL.createConnection("")) {
            AdminBootstrap.create(connection, "bootstrap-test", "Synthetic-test-password-2026");
            try (var rows = connection.createStatement().executeQuery("SELECT username,password_hash FROM admin_user")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("bootstrap-test");
                assertThat(new BCryptPasswordEncoder().matches("Synthetic-test-password-2026", rows.getString(2))).isTrue();
                assertThat(rows.next()).isFalse();
            }
            assertThatThrownBy(() -> AdminBootstrap.create(connection, "another-test", "Other-test-password-2026"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> AdminBootstrap.create(connection, "bootstrap-test", "Other-test-password-2026"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rejectsInvalidCredentialsBeforeDatabaseAccess() {
        assertThatThrownBy(() -> AdminBootstrap.create(null, "bad'username", "Synthetic-test-password-2026"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminBootstrap.create(null, "valid-user", "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdminBootstrap.create(null, "valid-user", "中".repeat(25)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
