package com.yongtuo.site.media;

/** Writes validated media bytes to the configured object-storage provider. */
public interface ObjectStorageUploadService {
    StoredObject upload(String storageKey, String originalName, String mimeType, byte[] content);

    /** Removes a stored object if present; implementations must make this operation idempotent. */
    void delete(String storageKey);
}
