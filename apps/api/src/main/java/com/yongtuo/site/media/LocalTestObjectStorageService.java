package com.yongtuo.site.media;

import java.net.URI;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Uses only an already-stored public URL in local/test environments; it never fabricates signatures. */
@Service
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "yongtuo.storage", name = "provider",
        havingValue = "local-public-url", matchIfMissing = true)
public class LocalTestObjectStorageService implements ObjectStorageService {
    @Override
    public Optional<String> resolveDownloadUrl(String storageKey, String storedPublicUrl) {
        if (storedPublicUrl == null || storedPublicUrl.isBlank()) return Optional.empty();
        try {
            URI uri = URI.create(storedPublicUrl.trim());
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || scheme == null || uri.getHost() == null || uri.getHost().isBlank()
                    || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                return Optional.empty();
            }
            return Optional.of(uri.toString());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
