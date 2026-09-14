package com.yongtuo.site.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Single-node V1 limiter. Reservations also count in-flight password checks. */
@Component
class LoginAttemptLimiter {
    private static final int LIMIT = 5;
    private static final int MAX_KEYS = 10000;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Clock clock;
    private final Map<String, Counter> attempts = new HashMap<>();

    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    synchronized boolean acquire(String account, String ip) {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        String accountKey = "account:" + account;
        String ipKey = "ip:" + ip;
        if (count(accountKey) >= LIMIT || count(ipKey) >= LIMIT) return false;
        int needed = (attempts.containsKey(accountKey) ? 0 : 1) + (attempts.containsKey(ipKey) ? 0 : 1);
        if (attempts.size() + needed > MAX_KEYS) return false;
        increment(accountKey, now);
        increment(ipKey, now);
        return true;
    }

    synchronized void succeeded(String account, String ip) {
        attempts.remove("account:" + account);
        attempts.remove("ip:" + ip);
    }

    private int count(String key) {
        Counter value = attempts.get(key);
        return value == null ? 0 : value.count();
    }

    private void increment(String key, Instant now) {
        Counter previous = attempts.get(key);
        attempts.put(key, previous == null ? new Counter(1, now.plus(WINDOW))
                : new Counter(previous.count() + 1, previous.expiresAt()));
    }

    private record Counter(int count, Instant expiresAt) { }
}
