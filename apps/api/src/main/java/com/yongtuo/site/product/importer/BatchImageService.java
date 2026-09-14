package com.yongtuo.site.product.importer;

import com.yongtuo.site.media.ObjectStorageUploadService;
import com.yongtuo.site.media.StoredObject;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BatchImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchImageService.class);
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_FILES = 100;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final Pattern FILE_NAME = Pattern.compile(
            "^(.{1,100})-([1-9][0-9]{0,9})\\.(jpe?g|png|webp)$",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> MIME_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");

    private final JdbcTemplate jdbc;
    private final ObjectStorageUploadService storage;

    public BatchImageService(JdbcTemplate jdbc, ObjectStorageUploadService storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Transactional
    public BatchImageReport upload(List<MultipartFile> files) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Batch image upload requires an active transaction");
        }
        List<MultipartFile> safeFiles = files == null ? List.of() : List.copyOf(files);
        if (safeFiles.size() > MAX_FILES || totalSize(safeFiles) > MAX_TOTAL_SIZE) {
            return rejectedBatch(safeFiles, "BATCH_LIMIT_EXCEEDED");
        }

        List<Candidate> candidates = new ArrayList<>();
        List<BatchImageFileResult> results = new ArrayList<>();
        for (MultipartFile file : safeFiles) {
            String fileName = file == null || file.getOriginalFilename() == null
                    ? "" : file.getOriginalFilename();
            Matcher matcher = safeFileName(fileName) ? FILE_NAME.matcher(fileName) : null;
            if (matcher == null || !matcher.matches()) {
                results.add(failed(fileName, null, null, "INVALID_FILENAME"));
                continue;
            }
            String extension = matcher.group(3).toLowerCase(Locale.ROOT);
            try {
                candidates.add(new Candidate(file, fileName, matcher.group(1),
                        Integer.parseInt(matcher.group(2)), extension));
            } catch (NumberFormatException exception) {
                results.add(failed(fileName, null, null, "INVALID_FILENAME"));
            }
        }
        candidates.sort(Comparator.comparing(Candidate::productCode)
                .thenComparingInt(Candidate::sequence)
                .thenComparing(Candidate::fileName));

        List<MatchedCandidate> matchedCandidates = new ArrayList<>();
        for (Candidate candidate : candidates) {
            List<Long> productIds = jdbc.queryForList("""
                    SELECT id FROM product
                    WHERE product_code = ? AND deleted_at IS NULL
                    """, Long.class, candidate.productCode());
            if (productIds.isEmpty()) {
                results.add(new BatchImageFileResult(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), BatchImageFileStatus.UNMATCHED, "PRODUCT_NOT_FOUND"));
                continue;
            }
            matchedCandidates.add(new MatchedCandidate(candidate, productIds.getFirst()));
        }

        Set<Long> lockedProductIds = lockProducts(matchedCandidates);
        List<MatchedCandidate> liveCandidates = new ArrayList<>();
        for (MatchedCandidate matched : matchedCandidates) {
            if (lockedProductIds.contains(matched.productId()) && productCodeStillMatches(matched)) {
                liveCandidates.add(matched);
            } else {
                Candidate candidate = matched.candidate();
                results.add(new BatchImageFileResult(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), BatchImageFileStatus.UNMATCHED, "PRODUCT_NOT_FOUND"));
            }
        }
        liveCandidates.sort(Comparator.comparingLong(MatchedCandidate::productId)
                .thenComparingInt(matched -> matched.candidate().sequence())
                .thenComparing(matched -> matched.candidate().fileName()));

        List<String> uploadedKeys = new ArrayList<>();
        registerRollbackCleanup(uploadedKeys);
        Map<ImageSlot, Integer> occurrences = new HashMap<>();
        liveCandidates.forEach(matched -> occurrences.merge(matched.slot(), 1, Integer::sum));
        Set<ImageSlot> existingSlots = existingSlots(liveCandidates);
        for (MatchedCandidate matched : liveCandidates) {
            Candidate candidate = matched.candidate();
            if (occurrences.get(matched.slot()) > 1) {
                results.add(failed(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), "DUPLICATE_SEQUENCE"));
                continue;
            }
            if (existingSlots.contains(matched.slot())) {
                results.add(failed(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), "SEQUENCE_ALREADY_EXISTS"));
                continue;
            }
            byte[] content = readAndValidate(candidate);
            if (content == null) {
                results.add(failed(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), "INVALID_IMAGE"));
                continue;
            }
            long productId = matched.productId();
            String storageKey = "product-images/" + productId + "/" + UUID.randomUUID()
                    + "." + candidate.extension();
            StoredObject stored;
            try {
                stored = validateStoredObject(storage.upload(storageKey, candidate.fileName(),
                        MIME_BY_EXTENSION.get(candidate.extension()), content), storageKey);
            } catch (RuntimeException exception) {
                deleteQuietly(storageKey, "failed upload metadata compensation");
                results.add(failed(candidate.fileName(), candidate.productCode(),
                        candidate.sequence(), "STORAGE_UPLOAD_FAILED"));
                continue;
            }
            uploadedKeys.add(stored.storageKey());
            long mediaId = insertMedia(candidate, content, stored);
            jdbc.update("""
                    INSERT INTO product_image(product_id,media_id,image_url,sort_order,is_cover)
                    VALUES (?,?,?,?,0)
                    """, productId, mediaId, stored.publicUrl(), candidate.sequence());
            results.add(new BatchImageFileResult(candidate.fileName(), candidate.productCode(),
                    candidate.sequence(), BatchImageFileStatus.SUCCESS, null));
        }

        results.sort(Comparator
                .comparing((BatchImageFileResult result) -> result.productCode() == null ? "" : result.productCode())
                .thenComparing(result -> result.sequence() == null ? 0 : result.sequence())
                .thenComparing(BatchImageFileResult::fileName));
        int success = (int) results.stream().filter(result -> result.status() == BatchImageFileStatus.SUCCESS).count();
        int failed = (int) results.stream().filter(result -> result.status() == BatchImageFileStatus.FAILED).count();
        int unmatched = results.size() - success - failed;
        return new BatchImageReport(results.size(), success, failed, unmatched, results);
    }

    private Set<Long> lockProducts(List<MatchedCandidate> candidates) {
        List<Long> productIds = candidates.stream().map(MatchedCandidate::productId)
                .distinct().sorted().toList();
        if (productIds.isEmpty()) return Set.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
        return new HashSet<>(jdbc.queryForList("""
                SELECT id FROM product
                WHERE id IN (%s) AND deleted_at IS NULL
                ORDER BY id FOR UPDATE
                """.formatted(placeholders), Long.class, productIds.toArray()));
    }

    private boolean productCodeStillMatches(MatchedCandidate matched) {
        return !jdbc.queryForList("""
                SELECT id FROM product
                WHERE id = ? AND product_code = ? AND deleted_at IS NULL
                FOR UPDATE
                """, Long.class, matched.productId(), matched.candidate().productCode()).isEmpty();
    }

    private Set<ImageSlot> existingSlots(List<MatchedCandidate> candidates) {
        Set<ImageSlot> existing = new HashSet<>();
        for (MatchedCandidate matched : candidates) {
            if (existing.contains(matched.slot())) continue;
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM product_image
                    WHERE product_id = ? AND sort_order = ?
                    """, Integer.class, matched.productId(), matched.candidate().sequence());
            if (count != null && count > 0) existing.add(matched.slot());
        }
        return existing;
    }

    private long insertMedia(Candidate candidate, byte[] content, StoredObject stored) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO media_file(
                      storage_key,original_name,public_url,file_type,mime_type,file_size,checksum,status
                    ) VALUES (?,?,?,'IMAGE',?,?,?,'ACTIVE')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, stored.storageKey());
            statement.setString(2, candidate.fileName());
            statement.setString(3, stored.publicUrl());
            statement.setString(4, MIME_BY_EXTENSION.get(candidate.extension()));
            statement.setLong(5, content.length);
            statement.setString(6, sha256(content));
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("Media insert did not return an id");
        return key.longValue();
    }

    private static StoredObject validateStoredObject(StoredObject stored, String requestedKey) {
        if (stored == null || !requestedKey.equals(stored.storageKey())
                || stored.storageKey().length() > 512 || stored.publicUrl() == null
                || stored.publicUrl().isBlank() || stored.publicUrl().length() > 1024) {
            throw new IllegalStateException("Object storage returned invalid metadata");
        }
        URI uri = URI.create(stored.publicUrl());
        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()
                || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            throw new IllegalStateException("Object storage returned an invalid public URL");
        }
        return stored;
    }

    private static byte[] readAndValidate(Candidate candidate) {
        MultipartFile file = candidate.file();
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE
                || !MIME_BY_EXTENSION.get(candidate.extension()).equalsIgnoreCase(file.getContentType())) {
            return null;
        }
        try {
            byte[] content = file.getBytes();
            return validSignature(candidate.extension(), content) ? content : null;
        } catch (IOException exception) {
            return null;
        }
    }

    private static boolean validSignature(String extension, byte[] content) {
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            return content.length >= 4 && unsigned(content[0]) == 0xff
                    && unsigned(content[1]) == 0xd8 && unsigned(content[2]) == 0xff;
        }
        if (extension.equals("png")) {
            byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            return content.length >= signature.length
                    && java.util.Arrays.equals(signature, java.util.Arrays.copyOf(content, signature.length));
        }
        return content.length >= 12 && ascii(content, 0, "RIFF") && ascii(content, 8, "WEBP");
    }

    private static boolean ascii(byte[] content, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            if (content[offset + index] != (byte) value.charAt(index)) return false;
        }
        return true;
    }

    private static int unsigned(byte value) {
        return value & 0xff;
    }

    private static boolean safeFileName(String fileName) {
        return !fileName.isBlank() && fileName.codePointCount(0, fileName.length()) <= 255
                && !fileName.contains("/") && !fileName.contains("\\")
                && fileName.codePoints().noneMatch(Character::isISOControl);
    }

    private static long totalSize(List<MultipartFile> files) {
        long total = 0;
        for (MultipartFile file : files) {
            if (file != null) {
                long size = Math.max(file.getSize(), 0);
                if (size > MAX_TOTAL_SIZE - total) return MAX_TOTAL_SIZE + 1;
                total += size;
            }
        }
        return total;
    }

    private void registerRollbackCleanup(List<String> uploadedKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;
                for (int index = uploadedKeys.size() - 1; index >= 0; index--) {
                    deleteQuietly(uploadedKeys.get(index), "transaction rollback compensation");
                }
            }
        });
    }

    private void deleteQuietly(String storageKey, String context) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Object storage cleanup failed for key {} during {}",
                    storageKey, context, exception);
        }
    }

    private static BatchImageReport rejectedBatch(List<MultipartFile> files, String reason) {
        List<BatchImageFileResult> results = files.stream().map(file -> failed(
                file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                null, null, reason)).toList();
        return new BatchImageReport(results.size(), 0, results.size(), 0, results);
    }

    private static BatchImageFileResult failed(String fileName, String productCode,
                                                Integer sequence, String reason) {
        return new BatchImageFileResult(fileName, productCode, sequence,
                BatchImageFileStatus.FAILED, reason);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record Candidate(MultipartFile file, String fileName, String productCode,
                             int sequence, String extension) {}

    private record MatchedCandidate(Candidate candidate, long productId) {
        private ImageSlot slot() {
            return new ImageSlot(productId, candidate.sequence());
        }
    }

    private record ImageSlot(long productId, int sequence) {}
}
