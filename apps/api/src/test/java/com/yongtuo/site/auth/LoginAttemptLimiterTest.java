package com.yongtuo.site.auth;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test void lockExpiresWithoutSleepingAndSuccessResetsBothKeys() {
        var clock = new MutableClock();
        var limiter = new LoginAttemptLimiter(clock);
        for (int i = 0; i < 5; i++) assertThat(limiter.acquire("account", "ip")).isTrue();
        assertThat(limiter.acquire("account", "other-ip")).isFalse();
        clock.now = clock.now.plusSeconds(901);
        assertThat(limiter.acquire("account", "ip")).isTrue();
        limiter.succeeded("account", "ip");
        for (int i = 0; i < 5; i++) assertThat(limiter.acquire("account", "ip")).isTrue();
        assertThat(limiter.acquire("other-account", "ip")).isFalse();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
