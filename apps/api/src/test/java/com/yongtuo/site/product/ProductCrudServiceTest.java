package com.yongtuo.site.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.category.AdminCategoryWriteRequest;
import com.yongtuo.site.category.CategoryMode;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.category.CategoryStatus;
import com.yongtuo.site.category.CategoryBusinessException;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductCrudServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired ProductService service;
    @Autowired CategoryService categoryService;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach
    void cleanProducts() {
        jdbc.update("DELETE FROM product_attachment");
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_image");
        jdbc.update("DELETE FROM product");
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");
    }

    @Test
    void createsDraftAndRejectsDuplicateProductCode() {
        long categoryId = category().id();
        ProductDto created = service.create(request(categoryId, "YT-RED-001", "red-anchor", "红色锚", "Red anchor"));

        assertThat(created.id()).isPositive();
        assertThat(created.productCode()).isEqualTo("YT-RED-001");
        assertThat(created.slug()).isEqualTo("red-anchor");
        assertThat(created.status()).isEqualTo(ProductStatus.DRAFT);
        assertThatThrownBy(() -> service.create(request(categoryId, "YT-RED-001", "another", "另一个", "Another")))
                .isInstanceOf(ProductBusinessException.class)
                .extracting("code").isEqualTo(20002);
    }

    @Test
    void updatesProductAndDuplicateCreatesNewUniqueDraftWithCopiedContent() {
        long categoryId = category().id();
        ProductDto original = service.create(request(categoryId, "YT-RED-002", "red-anchor-2", "原产品", "Original"));
        ProductDto updated = service.update(original.id(), new AdminProductUpdateRequest(
                categoryId, "YT-RED-002", "red-anchor-updated", "更新产品", "Updated", "更新摘要", "Updated summary",
                "更新详情", "Updated detail", EnglishStatus.CONFIRMED, null, false, 0, null, null, null, null, ProductStatus.PUBLISHED));

        assertThat(updated.englishStatus()).isEqualTo(EnglishStatus.CONFIRMED);
        ProductDto duplicate = service.duplicate(updated.id());
        assertThat(duplicate.id()).isNotEqualTo(updated.id());
        assertThat(duplicate.productCode()).isNotEqualTo(updated.productCode());
        assertThat(duplicate.slug()).isNotEqualTo(updated.slug());
        assertThat(duplicate.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(duplicate.nameZh()).isEqualTo("更新产品");
        assertThat(duplicate.descriptionZh()).isEqualTo("更新详情");
    }

    @Test
    void softDeleteHidesFromAdminAndPublicReads() {
        long categoryId = category().id();
        ProductDto product = service.create(request(categoryId, "YT-RED-003", "red-anchor-3", "可删除", "Deletable"));
        service.softDelete(product.id());

        assertThat(service.getAdminById(product.id())).isEmpty();
        assertThat(service.getAdminList()).noneMatch(item -> item.id() == product.id());
        assertThat(service.getPublicBySlug(product.slug(), Locale.CHINESE)).isEmpty();
        assertThat(service.getPublicBySlug(product.slug(), Locale.ENGLISH)).isEmpty();
        assertThat(service.getPublicList(Locale.CHINESE)).noneMatch(item -> item.id() == product.id());
        assertThat(service.getPublicList(Locale.ENGLISH)).noneMatch(item -> item.id() == product.id());
    }

    @Test
    void publishedProductWithoutConfirmedEnglishIsAbsentFromPublicRead() {
        long categoryId = category().id();
        ProductDto draftEnglish = service.create(request(categoryId, "YT-EN-001", "english-pending",
                "中文产品", "English product", ProductStatus.PUBLISHED, EnglishStatus.EMPTY));
        assertThat(draftEnglish.englishStatus()).isEqualTo(EnglishStatus.EMPTY);
        assertThat(service.getPublicBySlug(draftEnglish.slug(), Locale.CHINESE)).isPresent();
        assertThat(service.getPublicBySlug(draftEnglish.slug(), Locale.ENGLISH)).isEmpty();
        assertThat(service.getPublicList(Locale.CHINESE)).extracting(ProductDto::slug).contains(draftEnglish.slug());
        assertThat(service.getPublicList(Locale.ENGLISH)).extracting(ProductDto::slug).doesNotContain(draftEnglish.slug());

        ProductDto confirmed = service.create(request(categoryId, "YT-EN-002", "english-confirmed",
                "中文产品二", "Confirmed product", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        assertThat(service.getPublicBySlug(confirmed.slug(), Locale.ENGLISH)).isPresent();
        assertThat(service.getPublicList(Locale.ENGLISH)).extracting(ProductDto::slug).contains(confirmed.slug());
    }

    @Test
    void categoryCannotBeDeletedWhenItHasActiveOrSoftDeletedProduct() {
        long categoryId = category().id();
        ProductDto product = service.create(request(categoryId, "YT-RED-004", "red-anchor-4", "占用分类", "Used category"));

        assertThatThrownBy(() -> categoryService.softDelete(categoryId))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21002);
        service.softDelete(product.id());
        assertThatThrownBy(() -> categoryService.softDelete(categoryId))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21002);
    }

    @Test
    void categoryDeleteAndProductCreateSerializeOnCategoryLock() throws Exception {
        long categoryId = category().id();
        String lockName = "product-category-delete-create-race";
        jdbc.execute("DROP TRIGGER IF EXISTS product_insert_gate");
        jdbc.execute("""
                CREATE TRIGGER product_insert_gate BEFORE INSERT ON product
                FOR EACH ROW BEGIN
                    SET @product_insert_gate = GET_LOCK('product-category-delete-create-race', 10);
                    SET @product_insert_release = RELEASE_LOCK('product-category-delete-create-race');
                END
                """);
        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            namedLock(gate, "SELECT GET_LOCK(?, 2)", lockName);
            Future<ProductDto> create = executor.submit(() -> service.create(
                    request(categoryId, "YT-RACE-001", "race-product", "并发产品", "Concurrent product")));
            assertThatThrownBy(() -> create.get(750, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            Future<Void> delete = executor.submit(() -> {
                categoryService.softDelete(categoryId);
                return null;
            });
            assertThatThrownBy(() -> delete.get(750, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            assertThat(namedLock(gate, "SELECT RELEASE_LOCK(?)", lockName)).isEqualTo(1);
            assertThat(create.get(5, TimeUnit.SECONDS).id()).isPositive();
            assertThatThrownBy(() -> delete.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(java.util.concurrent.ExecutionException.class)
                    .cause().isInstanceOf(CategoryBusinessException.class)
                    .extracting("code").isEqualTo(21002);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS product_insert_gate");
        }
    }

    @Test
    void concurrentDuplicateUsesDatabaseUniquenessAndReturnsBusinessConflict() throws Exception {
        long categoryId = category().id();
        ProductDto original = service.create(request(categoryId, "YT-CONC-001", "concurrent-copy",
                "并发复制源", "Concurrent source"));
        String lockName = "product-duplicate-unique-race";
        jdbc.execute("DROP TRIGGER IF EXISTS product_duplicate_gate");
        jdbc.execute("""
                CREATE TRIGGER product_duplicate_gate BEFORE INSERT ON product
                FOR EACH ROW BEGIN
                    IF NEW.product_code LIKE 'YT-CONC-001-copy%' THEN
                        SET @product_duplicate_gate = GET_LOCK('product-duplicate-unique-race', 10);
                        SET @product_duplicate_release = RELEASE_LOCK('product-duplicate-unique-race');
                    END IF;
                END
                """);
        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            assertThat(namedLock(gate, "SELECT GET_LOCK(?, 2)", lockName)).isEqualTo(1);
            Future<ProductDto> first = executor.submit(() -> service.duplicate(original.id()));
            Future<ProductDto> second = executor.submit(() -> service.duplicate(original.id()));
            assertThatThrownBy(() -> first.get(750, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThatThrownBy(() -> second.get(750, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(namedLock(gate, "SELECT RELEASE_LOCK(?)", lockName)).isEqualTo(1);
            int successes = 0;
            int conflicts = 0;
            for (Future<ProductDto> future : List.of(first, second)) {
                try {
                    assertThat(future.get(5, TimeUnit.SECONDS).status()).isEqualTo(ProductStatus.DRAFT);
                    successes++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(ProductBusinessException.class)
                            .extracting("code").isEqualTo(20002);
                    conflicts++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE product_code LIKE 'YT-CONC-001-copy%'", Integer.class))
                    .isEqualTo(1);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS product_duplicate_gate");
        }
    }

    @Test
    void duplicateSlugReturnsDedicatedBusinessConflict() {
        long categoryId = category().id();
        service.create(request(categoryId, "YT-SLUG-001", "slug-collision", "第一个", "First"));

        assertThatThrownBy(() -> service.create(request(categoryId, "YT-SLUG-002", "slug-collision", "第二个", "Second")))
                .isInstanceOf(ProductBusinessException.class)
                .extracting("code").isEqualTo(20003);
    }

    @Test
    void unknownUniqueConstraintDoesNotGetMisreportedAsSlugConflict() {
        long categoryId = category().id();
        jdbc.execute("ALTER TABLE product ADD UNIQUE KEY uk_product_test_name (name_zh)");
        try {
            service.create(request(categoryId, "YT-UNKNOWN-001", "unknown-constraint-1", "唯一名称", "First"));
            assertThatThrownBy(() -> service.create(request(categoryId, "YT-UNKNOWN-002", "unknown-constraint-2", "唯一名称", "Second")))
                    .isInstanceOf(ProductBusinessException.class)
                    .extracting("code").isEqualTo(20005);
        } finally {
            jdbc.execute("ALTER TABLE product DROP INDEX uk_product_test_name");
        }
    }

    @Test
    void updateCodeAndSlugCollisionsUseDatabaseConstraintContracts() {
        long categoryId = category().id();
        ProductDto first = service.create(request(categoryId, "YT-UPDATE-001", "update-collision-1", "第一", "First"));
        ProductDto second = service.create(request(categoryId, "YT-UPDATE-002", "update-collision-2", "第二", "Second"));

        assertThatThrownBy(() -> service.update(second.id(), updateRequest(categoryId, first.productCode(), second.slug())))
                .isInstanceOf(ProductBusinessException.class)
                .extracting("code").isEqualTo(20002);
        assertThatThrownBy(() -> service.update(second.id(), updateRequest(categoryId, second.productCode(), first.slug())))
                .isInstanceOf(ProductBusinessException.class)
                .extracting("code").isEqualTo(20003);
    }

    @Test
    void mediaSchemaContainsMetadataAndNullableImageMediaForeignKey() {
        assertThat(columns("media_file")).contains("public_url", "file_type", "width", "height", "status");
        assertThat(columns("product_image")).contains("media_id");
        assertThat(columns("product_attachment")).contains("media_id");
        assertThat(jdbc.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'product_image' AND column_name = 'media_id'
                """, String.class)).isEqualTo("YES");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE() AND table_name = 'product_image'
                  AND column_name = 'media_id' AND referenced_table_name = 'media_file'
        """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE() AND table_name = 'product_attachment'
                  AND column_name = 'media_id' AND referenced_table_name = 'media_file'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void migrationContractIncludesChecksKeysDefaultsAndRestrictiveForeignKeys() {
        assertThat(jdbc.queryForObject("""
                SELECT column_type FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'english_status'
                """, String.class)).isEqualTo("varchar(16)");
        assertThat(jdbc.queryForObject("""
                SELECT column_default FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'english_status'
                """, String.class)).isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'product_variant_value' AND column_name = 'option_id'
                """, String.class)).isEqualTo("YES");
        assertThat(jdbc.queryForObject("""
                SELECT extra FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'active_product_code'
                """, String.class)).containsIgnoringCase("STORED GENERATED");
        assertThat(jdbc.queryForObject("""
                SELECT extra FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'media_file' AND column_name = 'active_storage_key'
                """, String.class)).containsIgnoringCase("STORED GENERATED");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'product'
                  AND index_name = 'uk_product_active_code' AND non_unique = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'product'
                  AND index_name = 'uk_product_active_slug' AND non_unique = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(indexColumns("product", "idx_product_category_status_deleted_sort"))
                .containsExactly("category_id", "status", "deleted_at", "sort_order");
        assertThat(indexColumns("product", "idx_product_featured_status_sort"))
                .containsExactly("is_featured", "status", "sort_order");
        assertThat(indexColumns("product", "idx_product_updated_at")).containsExactly("updated_at");

        assertThat(checkClauses("product")).anyMatch(clause -> clause.contains("english_status"));
        assertThat(checkClauses("media_file")).anyMatch(clause -> clause.contains("status"));
        assertThat(foreignKeyDeleteRule("product_image", "fk_product_image_media")).isEqualTo("RESTRICT");
        assertThat(foreignKeyDeleteRule("product_attachment", "fk_product_attachment_media")).isEqualTo("RESTRICT");
        long categoryId = category().id();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO product(category_id, product_code, name_zh, slug, english_status)
                VALUES (?, ?, ?, ?, ?)
                """, categoryId, "YT-MIGRATION-INVALID", "非法状态", "migration-invalid", "BROKEN"))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO media_file(storage_key, original_name, public_url, file_type, mime_type, file_size, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "invalid-status", "invalid", "https://invalid", "image", "image/png", 1L, "BROKEN"))
                .isInstanceOf(Exception.class);
        ProductDto product = service.create(request(categoryId, "YT-MIGRATION-FK", "migration-fk", "FK 产品", "FK product"));
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO product_image(product_id, media_id, image_url)
                VALUES (?, ?, ?)
                """, product.id(), -1L, "https://invalid"))
                .isInstanceOf(Exception.class);
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private List<String> indexColumns(String table, String index) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                ORDER BY seq_in_index
                """, String.class, table, index);
    }

    private List<String> checkClauses(String table) {
        return jdbc.queryForList("""
                SELECT cc.check_clause FROM information_schema.check_constraints cc
                JOIN information_schema.table_constraints tc
                  ON tc.constraint_schema = cc.constraint_schema AND tc.constraint_name = cc.constraint_name
                WHERE cc.constraint_schema = DATABASE() AND tc.table_name = ? AND tc.constraint_type = 'CHECK'
                """, String.class, table);
    }

    private String foreignKeyDeleteRule(String table, String constraint) {
        return jdbc.queryForObject("""
                SELECT rc.delete_rule FROM information_schema.referential_constraints rc
                WHERE rc.constraint_schema = DATABASE() AND rc.table_name = ? AND rc.constraint_name = ?
                """, String.class, table, constraint);
    }

    @Test
    void adminProductRoutesRequireAuthenticationAndExposeCrudContract() throws Exception {
        long categoryId = category().id();
        String body = """
                {"categoryId":%d,"productCode":"YT-RED-005","slug":"red-anchor-5",
                 "nameZh":"路由产品","nameEn":"Route product","englishStatus":"EMPTY","isFeatured":false,"sortOrder":0,"status":"DRAFT"}
                """.formatted(categoryId);
        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/products").with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productCode").value("YT-RED-005"));
        mvc.perform(get("/api/v1/admin/products").with(user("synthetic-admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].productCode").value("YT-RED-005"));
        long id = jdbc.queryForObject("SELECT id FROM product WHERE product_code = 'YT-RED-005'", Long.class);
        mvc.perform(get("/api/v1/admin/products/{id}", id).with(user("synthetic-admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        mvc.perform(put("/api/v1/admin/products/{id}", id).with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("路由产品", "更新路由产品")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nameZh").value("更新路由产品"));
        mvc.perform(post("/api/v1/admin/products/{id}/duplicate", id).with(user("synthetic-admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"));
        mvc.perform(delete("/api/v1/admin/products/{id}", id).with(user("synthetic-admin")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/products/{slug}", "red-anchor-5"))
                .andExpect(status().isNotFound());
    }

    private ProductCategoryDto category() {
        return new ProductCategoryDto(categoryService.create(new AdminCategoryWriteRequest(
                null, "测试分类", "Synthetic category", "synthetic-category-" + System.nanoTime(), null,
                null, null, CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE, false,
                null, null, null, null)));
    }

    private static AdminProductCreateRequest request(long categoryId, String code, String slug,
                                                     String nameZh, String nameEn) {
        return request(categoryId, code, slug, nameZh, nameEn, ProductStatus.DRAFT, EnglishStatus.EMPTY);
    }

    private static AdminProductCreateRequest request(long categoryId, String code, String slug,
                                                     String nameZh, String nameEn, ProductStatus status,
                                                     EnglishStatus englishStatus) {
        return new AdminProductCreateRequest(categoryId, code, slug, nameZh, nameEn,
                "摘要", "Summary", "详情", "Description", englishStatus, null, false, 0, status,
                null, null, null, null);
    }

    private static AdminProductUpdateRequest updateRequest(long categoryId, String code, String slug) {
        return new AdminProductUpdateRequest(categoryId, code, slug, "更新", "Updated", null, null,
                null, null, EnglishStatus.EMPTY, null, false, 0, null, null, null, null, ProductStatus.DRAFT);
    }

    private record ProductCategoryDto(long id) {
        ProductCategoryDto(com.yongtuo.site.category.AdminCategoryDto dto) { this(dto.id()); }
    }

    private static int namedLock(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }
}
