package com.yongtuo.site.product.importer;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ImportPreviewStore {
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_PREVIEWS = 200;
    private static final int TOKEN_BYTES = 24;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, Snapshot> previews = new ConcurrentHashMap<>();
    private final Set<String> claims = ConcurrentHashMap.newKeySet();
    private final AtomicLong insertionSequence = new AtomicLong();
    private final Clock clock;

    public ImportPreviewStore() {
        this(Clock.systemUTC());
    }

    ImportPreviewStore(Clock clock) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public synchronized String put(List<NormalizedProductRow> rows, boolean confirmable) {
        Instant now = clock.instant();
        removeExpired(now);
        while (previews.size() >= MAX_PREVIEWS) {
            Optional<java.util.Map.Entry<String, Snapshot>> oldestUnclaimed = previews.entrySet().stream()
                    .filter(entry -> !claims.contains(entry.getKey()))
                    .min(MapEntryOrder.INSTANCE);
            if (oldestUnclaimed.isEmpty()) break;
            var entry = oldestUnclaimed.orElseThrow();
            previews.remove(entry.getKey(), entry.getValue());
        }
        String token;
        do {
            byte[] value = new byte[TOKEN_BYTES];
            random.nextBytes(value);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } while (previews.containsKey(token));
        previews.put(token, new Snapshot(rows, confirmable, now, insertionSequence.incrementAndGet()));
        return token;
    }

    public synchronized Optional<Snapshot> find(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Snapshot snapshot = previews.get(token);
        if (snapshot == null) return Optional.empty();
        if (!claims.contains(token) && snapshot.createdAt().plus(TTL).isBefore(clock.instant())) {
            previews.remove(token, snapshot);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public synchronized Optional<Snapshot> claim(String token) {
        Optional<Snapshot> snapshot = find(token);
        if (snapshot.isEmpty() || !claims.add(token)) return Optional.empty();
        return snapshot;
    }

    public synchronized void release(String token, Snapshot snapshot) {
        if (previews.get(token) == snapshot) {
            previews.replace(token, snapshot, new Snapshot(snapshot.rows(), snapshot.confirmable(),
                    clock.instant(), snapshot.insertionSequence()));
        }
        claims.remove(token);
    }

    public synchronized void complete(String token, Snapshot snapshot) {
        previews.remove(token, snapshot);
        claims.remove(token);
    }

    synchronized void clear() {
        previews.clear();
        claims.clear();
    }

    private void removeExpired(Instant now) {
        previews.forEach((token, snapshot) -> {
            if (!claims.contains(token) && snapshot.createdAt().plus(TTL).isBefore(now)) {
                previews.remove(token, snapshot);
            }
        });
    }

    public record Snapshot(List<NormalizedProductRow> rows, boolean confirmable, Instant createdAt,
                           long insertionSequence) {
        public Snapshot {
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }

    private enum MapEntryOrder implements Comparator<java.util.Map.Entry<String, Snapshot>> {
        INSTANCE;

        @Override
        public int compare(java.util.Map.Entry<String, Snapshot> left,
                           java.util.Map.Entry<String, Snapshot> right) {
            int byTime = left.getValue().createdAt().compareTo(right.getValue().createdAt());
            return byTime != 0 ? byTime
                    : Long.compare(left.getValue().insertionSequence(), right.getValue().insertionSequence());
        }
    }
}
