package com.yongtuo.site.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit offline initialization only. Not a Spring bean or HTTP registration endpoint. */
public final class AdminBootstrap {
    private AdminBootstrap() { }

    public static void main(String[] args) {
        try {
            var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String username = input.readLine();
            String password = input.readLine();
            validate(username, password);
            try (var connection = DriverManager.getConnection(required("SPRING_DATASOURCE_URL"),
                    required("SPRING_DATASOURCE_USERNAME"), required("SPRING_DATASOURCE_PASSWORD"))) {
                create(connection, username, password);
            }
            System.out.println("Initial administrator created.");
        } catch (Exception exception) {
            // No exception text: JDBC errors may contain connection or credential details.
            System.err.println("Initialization refused or failed. Check credential rules, database health and whether an administrator already exists.");
            System.exit(1);
        }
    }

    static void create(Connection connection, String username, String password) throws SQLException {
        validate(username, password);
        String hash = new BCryptPasswordEncoder().encode(password);
        // A database-scoped advisory lock serializes even two concurrent empty-table bootstraps.
        try (var query = connection.prepareStatement("SELECT GET_LOCK(CONCAT(DATABASE(), ':admin-bootstrap'), 10)");
             var result = query.executeQuery()) {
            if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Initialization is busy");
        }
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            try (var query = connection.prepareStatement("SELECT id FROM admin_user LIMIT 1 FOR UPDATE");
                 var rows = query.executeQuery()) {
                if (rows.next()) throw new IllegalStateException("Administrator already exists");
            }
            try (var insert = connection.prepareStatement("INSERT INTO admin_user(username,password_hash) VALUES (?,?)")) {
                insert.setString(1, username);
                insert.setString(2, hash);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            try { connection.setAutoCommit(autoCommit); }
            finally {
                try (var release = connection.prepareStatement("SELECT RELEASE_LOCK(CONCAT(DATABASE(), ':admin-bootstrap'))")) {
                    release.execute();
                }
            }
        }
    }

    private static void validate(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{1,64}") || password == null
                || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.contains("\n") || password.contains("\r")) {
            throw new IllegalArgumentException("Invalid initial credentials");
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment variable");
        return value;
    }
}
