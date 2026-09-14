package com.yongtuo.site.product.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.product.ProductStatus;
import com.yongtuo.site.site.UrlRedirectService;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductImportConfirmTest {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired ProductImportService service;
    @Autowired ImportPreviewStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UrlRedirectService redirects;
    @Autowired MockMvc mvc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM url_redirect");
        jdbc.update("DELETE FROM product_attachment");
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_image");
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
        long categoryId = categoryId();
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                VALUES (?,?,?,?, 'DRAFT','EMPTY')
                """, categoryId, "YT-EXISTING", "existing-product", "已有产品");
        store.clear();
    }

    @Test
    void confirmsExactlyTenProductsFromServerSnapshotAndConsumesToken() throws Exception {
        ProductImportPreview preview = service.preview(workbook(rows("YT-CONFIRM", 10)));
        int before = productCount();

        ProductImportConfirmation confirmation = service.confirm(preview.importToken());

        assertThat(confirmation.imported()).isEqualTo(10);
        assertThat(productCount()).isEqualTo(before + 10);
        assertThatThrownBy(() -> service.confirm(preview.importToken()))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23005);
    }

    @Test
    void publishedImportRemovesStaleRedirectForReusedSlug() throws Exception {
        jdbc.update("""
                INSERT INTO url_redirect(old_path, new_path)
                VALUES ('/products/import-reused', '/products/historical-target')
                """);
        ProductImportPreview preview = service.preview(workbook(List.<String[]>of(new String[] {
                "YT-IMPORT-REDIRECT", "import-reused", "fasteners", "导入复用路径产品", "PUBLISHED"
        })));

        service.confirm(preview.importToken());

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM url_redirect WHERE old_path = '/products/import-reused'
                """, Integer.class)).isZero();
    }

    @Test
    void confirmEndpointRequiresAuthenticationAndRejectsUnknownOrUnconfirmableTokens() throws Exception {
        String unknown = "{\"importToken\":\"unknown-token\"}";
        mvc.perform(post("/api/v1/admin/products/import/confirm")
                        .contentType(APPLICATION_JSON).content(unknown))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/products/import/confirm")
                        .with(user("synthetic-admin"))
                        .contentType(APPLICATION_JSON).content(unknown))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(23005));

        String unconfirmable = store.put(List.of(), false);
        mvc.perform(post("/api/v1/admin/products/import/confirm")
                        .with(user("synthetic-admin"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"importToken\":\"" + unconfirmable + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(23006));
        assertThat(productCount()).isEqualTo(1);
    }

    @Test
    void rejectsExpiredPreviewToken() throws Exception {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-09-07T00:00:00Z"));
        ImportPreviewStore expiringStore = new ImportPreviewStore(clock);
        ProductImportService expiringService = new ProductImportService(
                jdbc, objectMapper, expiringStore, redirects);
        ProductImportPreview preview = expiringService.preview(workbook(rows("YT-EXPIRED", 1)));
        clock.advance(Duration.ofMinutes(31));

        assertThatThrownBy(() -> expiringService.confirm(preview.importToken()))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23005);
        assertThat(productCount()).isEqualTo(1);
    }

    @Test
    void rollsBackEveryInsertedProductOnDatabaseConflictAndAllowsRetry() throws Exception {
        ProductImportPreview preview = service.preview(workbook(rows("YT-ROLLBACK", 3)));
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                VALUES (?,?,?,?, 'DRAFT','EMPTY')
                """, categoryId(), "YT-ROLLBACK-2", "external-conflict", "并发冲突");
        int before = productCount();

        assertThatThrownBy(() -> service.confirm(preview.importToken()))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23007);
        assertThat(productCount()).isEqualTo(before);

        jdbc.update("DELETE FROM product WHERE product_code='YT-ROLLBACK-2'");
        assertThat(service.confirm(preview.importToken()).imported()).isEqualTo(3);
        assertThat(productCount()).isEqualTo(before + 2);
    }

    @Test
    void claimedPreviewSurvivesExpiryAndCapacityPressureUntilRollbackRelease() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-09-07T00:00:00Z"));
        ImportPreviewStore isolated = new ImportPreviewStore(clock);
        NormalizedProductRow row = new NormalizedProductRow(
                2, categoryId(), "YT-CLAIMED", "yt-claimed", "占用中的产品", ProductStatus.DRAFT);
        String token = isolated.put(List.of(row), true);
        ImportPreviewStore.Snapshot snapshot = isolated.claim(token).orElseThrow();

        clock.advance(Duration.ofMinutes(31));
        IntStream.range(0, 201).forEach(index -> isolated.put(List.of(row), true));

        assertThat(isolated.find(token)).contains(snapshot);
        isolated.release(token, snapshot);
        assertThat(isolated.claim(token)).isPresent();
    }

    @Test
    void rejectsSoftDeletedCategoryWithoutPartialInsertAndAllowsRetry() throws Exception {
        ProductImportPreview preview = service.preview(workbook(rows("YT-CATEGORY-CHANGED", 3)));
        int before = productCount();
        jdbc.update("UPDATE product_category SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", categoryId());

        assertThatThrownBy(() -> service.confirm(preview.importToken()))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23007);
        assertThat(productCount()).isEqualTo(before);

        jdbc.update("UPDATE product_category SET deleted_at = NULL WHERE slug = 'fasteners'");
        assertThat(service.confirm(preview.importToken()).imported()).isEqualTo(3);
        assertThat(productCount()).isEqualTo(before + 3);
    }

    @Test
    void permitsOnlyOneConcurrentConfirmationForTheSameToken() throws Exception {
        ProductImportPreview preview = service.preview(workbook(rows("YT-CONCURRENT", 5)));
        int before = productCount();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> confirmAfterStart(preview.importToken(), ready, start));
            var second = executor.submit(() -> confirmAfterStart(preview.importToken(), ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(0, 23005);
            assertThat(productCount()).isEqualTo(before + 5);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmEndpointRejectsOverlongTokenBeforeStoreLookup() throws Exception {
        mvc.perform(post("/api/v1/admin/products/import/confirm")
                        .with(user("synthetic-admin"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"importToken\":\"" + "a".repeat(65) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private int confirmAfterStart(String token, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        try {
            service.confirm(token);
            return 0;
        } catch (ProductImportException exception) {
            return exception.getCode();
        }
    }

    private long categoryId() {
        return jdbc.queryForObject("SELECT id FROM product_category WHERE slug='fasteners'", Long.class);
    }

    private int productCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class);
    }

    private static List<String[]> rows(String prefix, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new String[] {prefix + "-" + index,
                        prefix.toLowerCase() + "-" + index, "fasteners",
                        "导入产品" + index, "DRAFT"})
                .toList();
    }

    private static MockMultipartFile workbook(List<String[]> values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Products");
            Row header = sheet.createRow(0);
            List.of("productCode", "slug", "categorySlug", "nameZh", "status")
                    .forEach(value -> header.createCell(header.getLastCellNum() < 0 ? 0 : header.getLastCellNum())
                            .setCellValue(value));
            int rowIndex = 1;
            for (String[] rowValues : values) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < rowValues.length; column++) {
                    row.createCell(column).setCellValue(rowValues[column]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "products.xlsx", XLSX, output.toByteArray());
        }
    }

    private static final class AdjustableClock extends Clock {
        private Instant current;

        private AdjustableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
