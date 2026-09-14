package com.yongtuo.site.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class LoginAuditService {
    private final JdbcTemplate jdbc;
    LoginAuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    void record(AdminAccount account, String username, String ip, String userAgent, String failure) {
        jdbc.update("""
                INSERT INTO admin_login_log(admin_user_id, username, login_success,
                    ip_address, user_agent, failure_reason) VALUES (?, ?, ?, ?, ?, ?)
                """, account == null ? null : account.id(), clean(username, 100), failure == null,
                clean(ip, 45), clean(userAgent, 512), failure);
    }

    private static String clean(String value, int max) {
        if (value == null) return null;
        String cleaned = value.replaceAll("[\\p{Cntrl}]", " ");
        return cleaned.substring(0, Math.min(cleaned.length(), max));
    }
}
