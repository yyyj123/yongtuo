package com.yongtuo.site.product.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.YongtuoApiApplication;
import com.yongtuo.site.media.ObjectStorageUploadService;
import com.yongtuo.site.media.StoredObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(classes = {YongtuoApiApplication.class, BatchImageServiceTest.StorageConfig.class})
@AutoConfigureMockMvc
@Testcontainers
class BatchImageServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired BatchImageService service;
    @Autowired RecordingStorage storage;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM product_attachment");
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_image");
        jdbc.update("DELETE FROM media_file");
        jdbc.update("DELETE FROM product_attribute_value");
        jdbc.update("DELETE FROM category_attribute");
        jdbc.update("DELETE FROM attribute_option");
        jdbc.update("DELETE FROM attribute_definition");
        jdbc.update("DELETE FROM product");
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");
        jdbc.update("""
                INSERT INTO product_category(name_zh,name_en,slug,category_mode,status)
                VALUES ('测试紧固件','Synthetic fasteners','fasteners','NORMAL','ACTIVE')
                """);
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                SELECT id,'YT001','yt001','测试产品','DRAFT','EMPTY'
                FROM product_category WHERE slug='fasteners'
                """);
        storage.clear();
    }

    @Test
    void matchesUploadsAndOrdersImagesByProductCodeAndSequence() {
        BatchImageReport report = service.upload(List.of(
                jpeg("YT001-2.jpg"), jpeg("YT001-1.jpg")));

        assertThat(report.total()).isEqualTo(2);
        assertThat(report.success()).isEqualTo(2);
        assertThat(report.failed()).isZero();
        assertThat(report.unmatched()).isZero();
        assertThat(report.files()).extracting(BatchImageFileResult::sequence)
                .containsExactly(1, 2);
        assertThat(storage.uploadedNames).containsExactly("YT001-1.jpg", "YT001-2.jpg");
        assertThat(jdbc.queryForList("""
                SELECT media.original_name
                FROM product_image image
                JOIN media_file media ON media.id = image.media_id
                ORDER BY image.sort_order, image.id
                """, String.class)).containsExactly("YT001-1.jpg", "YT001-2.jpg");
        assertThat(jdbc.queryForList("SELECT sort_order FROM product_image ORDER BY sort_order", Integer.class))
                .containsExactly(1, 2);
        assertThat(storage.deletedKeys).isEmpty();
    }

    @Test
    void reportsMissingProductWithoutUploadingOrBindingFile() {
        BatchImageReport report = service.upload(List.of(jpeg("MISSING-1.jpg")));

        assertThat(report.success()).isZero();
        assertThat(report.failed()).isZero();
        assertThat(report.unmatched()).isEqualTo(1);
        assertThat(report.files().getFirst().reason()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(storage.uploadedNames).isEmpty();
        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
    }

    @Test
    void reportsInvalidFilenameWithoutUploadingOrBindingFile() {
        BatchImageReport report = service.upload(List.of(jpeg("YT001.jpg")));

        assertThat(report.success()).isZero();
        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.unmatched()).isZero();
        assertThat(report.files().getFirst().reason()).isEqualTo("INVALID_FILENAME");
        assertThat(storage.uploadedNames).isEmpty();
        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
    }

    @Test
    void rejectsMimeOrSignatureMismatchWithoutUploading() {
        MockMultipartFile disguised = new MockMultipartFile(
                "files", "YT001-1.jpg", "image/png", new byte[] {1, 2, 3, 4});

        BatchImageReport report = service.upload(List.of(disguised));

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.files().getFirst().reason()).isEqualTo("INVALID_IMAGE");
        assertThat(storage.uploadedNames).isEmpty();
        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
    }

    @Test
    void rejectsDuplicateSequenceWithoutUploadingEitherFile() {
        BatchImageReport report = service.upload(List.of(
                jpeg("YT001-1.jpg"), jpeg("YT001-1.jpeg")));

        assertThat(report.failed()).isEqualTo(2);
        assertThat(report.files()).extracting(BatchImageFileResult::reason)
                .containsOnly("DUPLICATE_SEQUENCE");
        assertThat(storage.uploadedNames).isEmpty();
        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
    }

    @Test
    void rejectsDuplicateSequenceUsingDatabaseProductCodeCollation() {
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                SELECT id,'YT-CAFÉ','yt-cafe','重音测试产品','DRAFT','EMPTY'
                FROM product_category WHERE slug='fasteners'
                """);

        BatchImageReport report = service.upload(List.of(
                jpeg("YT-CAFE-1.jpg"), jpeg("YT-CAFÉ-1.jpeg")));

        assertThat(report.failed()).isEqualTo(2);
        assertThat(report.files()).extracting(BatchImageFileResult::reason)
                .containsOnly("DUPLICATE_SEQUENCE");
        assertThat(storage.uploadedNames).isEmpty();
        assertThat(imageCount()).isZero();
    }

    @Test
    void rejectsSequenceAlreadyBoundByAnEarlierBatch() {
        assertThat(service.upload(List.of(jpeg("YT001-1.jpg"))).success()).isEqualTo(1);

        BatchImageReport report = service.upload(List.of(jpeg("YT001-1.jpeg")));

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.files().getFirst().reason()).isEqualTo("SEQUENCE_ALREADY_EXISTS");
        assertThat(storage.uploadedNames).containsExactly("YT001-1.jpg");
        assertThat(imageCount()).isEqualTo(1);
    }

    @Test
    void doesNotBindWhenProductCodeChangesBetweenLookupAndLock() throws Exception {
        CountDownLatch codeUpdated = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        AtomicReference<Thread> uploadThread = new AtomicReference<>();
        ExecutorService renameExecutor = Executors.newSingleThreadExecutor();
        ExecutorService uploadExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "batch-image-race-upload");
            uploadThread.set(thread);
            return thread;
        });
        try {
            Future<?> rename = renameExecutor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        jdbc.update("UPDATE product SET product_code='YT002' WHERE product_code='YT001'");
                        codeUpdated.countDown();
                        await(allowCommit);
                    }));
            assertThat(codeUpdated.await(5, TimeUnit.SECONDS)).isTrue();

            Future<BatchImageReport> upload = uploadExecutor.submit(
                    () -> service.upload(List.of(jpeg("YT001-1.jpg"))));
            awaitProductLock(uploadThread);
            allowCommit.countDown();
            rename.get(5, TimeUnit.SECONDS);

            BatchImageReport report = upload.get(5, TimeUnit.SECONDS);
            assertThat(report.unmatched()).isEqualTo(1);
            assertThat(report.files().getFirst().reason()).isEqualTo("PRODUCT_NOT_FOUND");
            assertThat(storage.uploadedNames).isEmpty();
            assertThat(imageCount()).isZero();
            assertThat(mediaCount()).isZero();
        } finally {
            allowCommit.countDown();
            renameExecutor.shutdownNow();
            uploadExecutor.shutdownNow();
        }
    }

    @Test
    void deletesUploadedObjectWhenProviderMetadataIsInvalid() {
        storage.invalidMetadata = true;

        BatchImageReport report = service.upload(List.of(jpeg("YT001-1.jpg")));

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.files().getFirst().reason()).isEqualTo("STORAGE_UPLOAD_FAILED");
        assertThat(storage.deletedKeys).hasSize(1);
        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
    }

    @Test
    void databaseFailureRollsBackWholeBatchAndDeletesEveryUploadedObject() {
        jdbc.execute("CREATE UNIQUE INDEX test_product_image_url ON product_image(image_url(191))");
        storage.fixedPublicUrl = "https://assets.invalid/same.jpg";
        try {
            assertThatThrownBy(() -> service.upload(List.of(
                    jpeg("YT001-1.jpg"), jpeg("YT001-2.jpg"))))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("DROP INDEX test_product_image_url ON product_image");
        }

        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
        assertThat(storage.uploadedNames).containsExactly("YT001-1.jpg", "YT001-2.jpg");
        assertThat(storage.deletedKeys).hasSize(2)
                .containsExactlyInAnyOrderElementsOf(storage.uploadedKeys);
    }

    @Test
    void outerTransactionRollbackDeletesObjectAndDatabaseRows() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(service.upload(List.of(jpeg("YT001-1.jpg"))).success()).isEqualTo(1);
            status.setRollbackOnly();
        });

        assertThat(imageCount()).isZero();
        assertThat(mediaCount()).isZero();
        assertThat(storage.deletedKeys).containsExactlyElementsOf(storage.uploadedKeys);
    }

    @Test
    void batchEndpointRequiresAuthenticationAndReportsUnmatchedReason() throws Exception {
        MockMultipartFile missing = jpeg("MISSING-1.jpg");
        mvc.perform(multipart("/api/v1/admin/products/images/batch").file(missing))
                .andExpect(status().isUnauthorized());

        mvc.perform(multipart("/api/v1/admin/products/images/batch")
                        .file(missing).with(user("synthetic-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.unmatched").value(1))
                .andExpect(jsonPath("$.data.files[0].reason").value("PRODUCT_NOT_FOUND"));
    }

    private int imageCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product_image", Integer.class);
    }

    private int mediaCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM media_file", Integer.class);
    }

    private static void awaitProductLock(AtomicReference<Thread> uploadThread)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Thread thread = uploadThread.get();
            if (thread != null && java.util.Arrays.stream(thread.getStackTrace())
                    .anyMatch(frame -> frame.getClassName().equals(BatchImageService.class.getName())
                            && frame.getMethodName().equals("lockProducts"))) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for batch upload to block on the product row");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for concurrent test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Concurrent test coordination was interrupted", exception);
        }
    }

    private static MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("files", name, "image/jpeg",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});
    }

    private static final class RecordingStorage implements ObjectStorageUploadService {
        private final List<String> uploadedNames = new ArrayList<>();
        private final List<String> uploadedKeys = new ArrayList<>();
        private final List<String> deletedKeys = new ArrayList<>();
        private boolean invalidMetadata;
        private String fixedPublicUrl;

        @Override
        public StoredObject upload(String storageKey, String originalName, String mimeType, byte[] content) {
            uploadedNames.add(originalName);
            uploadedKeys.add(storageKey);
            return new StoredObject(storageKey, invalidMetadata ? "not-a-public-url"
                    : fixedPublicUrl == null ? "https://assets.invalid/" + originalName : fixedPublicUrl);
        }

        @Override
        public void delete(String storageKey) {
            deletedKeys.add(storageKey);
        }

        private void clear() {
            uploadedNames.clear();
            uploadedKeys.clear();
            deletedKeys.clear();
            invalidMetadata = false;
            fixedPublicUrl = null;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StorageConfig {
        @Bean
        @Primary
        RecordingStorage recordingStorage() {
            return new RecordingStorage();
        }
    }
}
