package com.yongtuo.site.media;

import java.util.Optional;

/** Resolves a stored object to a URL suitable for an explicitly allowed public download. */
public interface ObjectStorageService {
    Optional<String> resolveDownloadUrl(String storageKey, String storedPublicUrl);
}
