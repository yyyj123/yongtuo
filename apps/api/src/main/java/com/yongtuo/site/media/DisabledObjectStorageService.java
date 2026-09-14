package com.yongtuo.site.media;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Safe fallback until a production object-storage adapter is explicitly configured. */
@Service
@Profile("!local & !test")
@ConditionalOnProperty(prefix = "yongtuo.storage", name = "provider",
        havingValue = "disabled", matchIfMissing = true)
public class DisabledObjectStorageService implements ObjectStorageService {
    @Override
    public Optional<String> resolveDownloadUrl(String storageKey, String storedPublicUrl) {
        return Optional.empty();
    }
}
