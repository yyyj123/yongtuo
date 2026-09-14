package com.yongtuo.site.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yongtuo.site.attribute.AttributeDataType;
import com.yongtuo.site.attribute.AttributeStatus;
import com.yongtuo.site.attribute.AdminAttributeDto;
import com.yongtuo.site.attribute.AdminAttributeOptionRequest;
import com.yongtuo.site.attribute.AdminAttributeWriteRequest;
import com.yongtuo.site.attribute.CategoryAttributeBindingRequest;
import com.yongtuo.site.category.AdminCategoryDto;
import com.yongtuo.site.category.AdminCategoryWriteRequest;
import com.yongtuo.site.category.CategoryMode;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.category.CategoryStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductAttributeAndVariantTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45")
            .withCommand("--log-bin-trust-function-creators=1");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired ProductService products;
    @Autowired CategoryService categories;
    @Autowired com.yongtuo.site.attribute.AttributeDefinitionService definitions;
    @Autowired com.yongtuo.site.attribute.CategoryAttributeService bindings;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach void clean() {
        jdbc.execute("DROP TRIGGER IF EXISTS product_attribute_value_fail_replacement");
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_attribute_value");
        jdbc.update("DELETE FROM product");
        jdbc.update("DELETE FROM category_attribute");
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");
        jdbc.update("DELETE FROM attribute_option");
        jdbc.update("DELETE FROM attribute_definition");
    }

    @Test void syntheticBoltStoresTypedAttributesAndThreeVariants() {
        long category = category("synthetic-bolt-category");
        AdminAttributeDto material = attribute("material", AttributeDataType.TEXT, true, false, false, List.of());
        AdminAttributeDto standard = attribute("standard", AttributeDataType.SELECT, true, true, false,
                List.of(option("DIN"), option("ISO")));
        AdminAttributeDto diameter = attribute("diameter", AttributeDataType.NUMBER, true, true, true, List.of());
        bindings.replaceCategoryBindings(category, List.of(
                binding(material, false, true, 20), binding(standard, true, false, 10), binding(diameter, true, true, 30)));
        long din = standard.options().getFirst().id();
        AdminProductCreateRequest request = request(category, List.of(
                new AdminProductAttributeRequest(material.id(), "测试用不锈钢", "Synthetic stainless steel", null, null, List.of(), 20),
                new AdminProductAttributeRequest(standard.id(), null, null, null, din, List.of(), 10),
                new AdminProductAttributeRequest(diameter.id(), null, null, new BigDecimal("12.500000"), null, List.of(), 30)),
                List.of(variant("BOLT-A", "规格 A", 0, diameter.id(), "10"),
                        variant("BOLT-B", "规格 B", 1, diameter.id(), "12"),
                        variant("BOLT-C", "规格 C", 2, diameter.id(), "14")));

        ProductDto created = products.create(request);

        assertThat(created.attributes()).extracting(ProductAttributeDto::code)
                .containsExactly("standard", "material", "diameter");
        assertThat(created.attributes()).extracting(ProductAttributeDto::numericValue)
                .containsExactly(null, null, new BigDecimal("12.500000"));
        assertThat(created.variants()).extracting(ProductVariantDto::variantCode)
                .containsExactly("BOLT-A", "BOLT-B", "BOLT-C");
        assertThat(created.variants()).allSatisfy(variant -> assertThat(variant.values()).hasSize(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_attribute_value WHERE product_id = ?", Integer.class, created.id()))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_attribute_value WHERE product_id = ? AND attribute_id IN (?, ?, ?) AND value_key = 0",
                Integer.class, created.id(), material.id(), standard.id(), diameter.id())).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id IN (SELECT id FROM product_variant WHERE product_id = ?)",
                Integer.class, created.id())).isEqualTo(3);
    }

    @Test void invalidVariantRollsBackProductAndComposition() {
        long category = category("synthetic-rollback-category");
        AdminAttributeDto material = attribute("rollback-material", AttributeDataType.TEXT, true, false, true, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(material, false, true, 0)));
        AdminProductCreateRequest request = request(category, List.of(),
                List.of(variant("DUP", "first", 0), variant("DUP", "duplicate", 1)));

        assertThatThrownBy(() -> products.create(request)).isInstanceOf(ProductBusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE product_code = ?", Integer.class, "SYN-BOLT-RED"))
                .isZero();
    }

    @Test void updateReplacesAttributesAndVariantsAtomically() {
        long category = category("synthetic-replacement-category");
        AdminAttributeDto material = attribute("replacement-material", AttributeDataType.TEXT, true, false, true, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(material, false, true, 0)));
        ProductDto created = products.create(request(category,
                List.of(new AdminProductAttributeRequest(material.id(), "旧材质", null, null, null, List.of(), 0)),
                List.of(variant("OLD-1", "old", 0))));

        ProductDto updated = products.update(created.id(), new AdminProductUpdateRequest(category, created.productCode(),
                created.slug(), created.nameZh(), created.nameEn(), null, null, null, null, EnglishStatus.CONFIRMED,
                null, false, 0, null, null, null, null, ProductStatus.DRAFT,
                List.of(new AdminProductAttributeRequest(material.id(), "新材质", null, null, null, List.of(), 0)),
                List.of(variant("NEW-1", "new", 0), variant("NEW-2", "new 2", 1))));

        assertThat(updated.attributes()).extracting(ProductAttributeDto::valueZh).containsExactly("新材质");
        assertThat(updated.variants()).extracting(ProductVariantDto::variantCode).containsExactly("NEW-1", "NEW-2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant WHERE product_id = ? AND variant_code = 'OLD-1'",
                Integer.class, created.id())).isZero();
    }

    @Test void requiredAttributeMayBeVariantOnlyButEveryVariantMustProvideIt() {
        long category = category("synthetic-required-inheritance-category");
        AdminAttributeDto required = attribute("required-variant-material", AttributeDataType.TEXT, true, false, true, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(required, false, true, 0)));
        ProductDto created = products.create(request(category, List.of(), List.of(
                variantText("REQ-1", "steel", required.id()), variantText("REQ-2", "alloy", required.id()))));

        assertThat(created.variants()).allSatisfy(variant -> assertThat(variant.values()).hasSize(1));
        AdminProductUpdateRequest invalid = updateRequest(created, category, List.of(),
                List.of(variantText("REQ-1", "steel", required.id()), variant("REQ-MISSING", "missing", 1)));
        assertThatThrownBy(() -> products.update(created.id(), invalid)).isInstanceOf(ProductBusinessException.class);

        ProductDto after = products.getAdminById(created.id()).orElseThrow();
        assertThat(after.productCode()).isEqualTo(created.productCode());
        assertThat(after.variants()).extracting(ProductVariantDto::variantCode).containsExactly("REQ-1", "REQ-2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id IN (SELECT id FROM product_variant WHERE product_id = ?)",
                Integer.class, created.id())).isEqualTo(2);
    }

    @Test void deterministicMidWriteFailureRollsBackProductAndOldComposition() {
        long category = category("synthetic-trigger-rollback-category");
        AdminAttributeDto text = attribute("trigger-material", AttributeDataType.TEXT, true, false, false, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(text, false, false, 0)));
        ProductDto created = products.create(request(category,
                List.of(new AdminProductAttributeRequest(text.id(), "old", null, null, null, List.of(), 0)),
                List.of(variant("OLD-TRIGGER", "old", 0))));
        jdbc.execute("""
                CREATE TRIGGER product_attribute_value_fail_replacement
                BEFORE INSERT ON product_attribute_value
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'deterministic replacement failure'
                """);

        AdminProductUpdateRequest replacement = updateRequest(created, category,
                List.of(new AdminProductAttributeRequest(text.id(), "replacement", null, null, null, List.of(), 0)),
                List.of(variant("NEW-TRIGGER", "new", 0)));
        assertThatThrownBy(() -> products.update(created.id(), replacement)).isInstanceOf(ProductBusinessException.class);

        ProductDto after = products.getAdminById(created.id()).orElseThrow();
        assertThat(after.productCode()).isEqualTo(created.productCode());
        assertThat(after.attributes()).extracting(ProductAttributeDto::valueZh).containsExactly("old");
        assertThat(after.variants()).extracting(ProductVariantDto::variantCode).containsExactly("OLD-TRIGGER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_attribute_value WHERE product_id = ?", Integer.class, created.id()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant WHERE product_id = ?", Integer.class, created.id()))
                .isEqualTo(1);
    }

    @Test void inactiveAttributeCommittedWhileProductWriteWaitsThenFailsSafely() throws Exception {
        long category = category("synthetic-inactive-concurrency-category");
        AdminAttributeDto text = attribute("concurrent-material", AttributeDataType.TEXT, true, false, false, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(text, false, false, 0)));
        ProductDto created = products.create(request(category,
                List.of(new AdminProductAttributeRequest(text.id(), "old", null, null, null, List.of(), 0)), List.of()));

        CountDownLatch attributeLocked = new CountDownLatch(1);
        CountDownLatch allowInactiveCommit = new CountDownLatch(1);
        CountDownLatch writerStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> lockFuture = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT id FROM attribute_definition WHERE id = ? FOR UPDATE", Long.class, text.id());
            jdbc.update("UPDATE attribute_definition SET status = 'INACTIVE' WHERE id = ?", text.id());
            attributeLocked.countDown();
            try {
                allowInactiveCommit.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }));
        Future<?> writer = null;
        try {
            assertThat(attributeLocked.await(5, TimeUnit.SECONDS)).isTrue();
            writer = executor.submit(() -> {
                writerStarted.countDown();
                return products.update(created.id(), updateRequest(created, category,
                        List.of(new AdminProductAttributeRequest(text.id(), "new", null, null, null, List.of(), 0)), List.of()));
            });
            assertThat(writerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> blockedWriter = writer;
            assertThatThrownBy(() -> blockedWriter.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            allowInactiveCommit.countDown();
            lockFuture.get(5, TimeUnit.SECONDS);
            try {
                writer.get(5, TimeUnit.SECONDS);
                throw new AssertionError("inactive attribute write unexpectedly succeeded");
            } catch (ExecutionException exception) {
                assertThat(exception.getCause()).isInstanceOf(ProductBusinessException.class);
            }
        } finally {
            allowInactiveCommit.countDown();
            if (writer != null) writer.cancel(true);
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        ProductDto after = products.getAdminById(created.id()).orElseThrow();
        assertThat(after.productCode()).isEqualTo(created.productCode());
        assertThat(after.attributes()).extracting(ProductAttributeDto::valueZh).containsExactly("old");
        assertThat(jdbc.queryForObject("SELECT status FROM attribute_definition WHERE id = ?", String.class, text.id()))
                .isEqualTo("INACTIVE");
    }

    @Test void multiSelectRequiresDistinctActiveOptionsAndPersistsNormalizedSelection() {
        long category = category("synthetic-multi-category");
        AdminAttributeDto finish = attribute("finish", AttributeDataType.MULTI_SELECT, true, true, true,
                List.of(option("zinc"), option("black")));
        bindings.replaceCategoryBindings(category, List.of(binding(finish, true, true, 0)));
        List<Long> ids = finish.options().stream().map(com.yongtuo.site.attribute.AdminAttributeOptionDto::id).toList();
        ProductDto created = products.create(request(category,
                List.of(new AdminProductAttributeRequest(finish.id(), null, null, null, null, List.of(ids.get(1), ids.get(0)), 0)),
                List.of()));
        assertThat(created.attributes().getFirst().optionIds()).containsExactly(ids.get(0), ids.get(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_attribute_value WHERE product_id = ?", Integer.class, created.id()))
                .isEqualTo(2);
        assertThatThrownBy(() -> products.update(created.id(), new AdminProductUpdateRequest(category, created.productCode(),
                created.slug(), created.nameZh(), created.nameEn(), null, null, null, null, EnglishStatus.CONFIRMED,
                null, false, 0, null, null, null, null, ProductStatus.DRAFT,
                List.of(new AdminProductAttributeRequest(finish.id(), null, null, null, null, List.of(ids.get(0), ids.get(0)), 0)),
                List.of()))).isInstanceOf(ProductBusinessException.class);
        assertThat(jdbc.queryForList("SELECT option_id FROM product_attribute_value WHERE product_id = ? ORDER BY id",
                Long.class, created.id())).containsExactlyElementsOf(ids.stream().sorted().toList());
    }

    @Test void variantValuesSupportAllFourTypesAndGlobalOverride() {
        long category = category("synthetic-four-types-category");
        AdminAttributeDto text = attribute("variant-text", AttributeDataType.TEXT, false, false, false, List.of());
        AdminAttributeDto number = attribute("variant-number", AttributeDataType.NUMBER, false, false, false, List.of());
        AdminAttributeDto select = attribute("variant-select", AttributeDataType.SELECT, false, false, false,
                List.of(option("one"), option("two")));
        AdminAttributeDto multi = attribute("variant-multi", AttributeDataType.MULTI_SELECT, false, false, false,
                List.of(option("a"), option("b")));
        AdminAttributeDto global = attribute("global-override", AttributeDataType.TEXT, true, false, true, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(text, false, false, 1), binding(number, false, false, 2),
                binding(select, false, false, 3), binding(multi, false, false, 4), binding(global, false, false, 5)));
        assertThat(bindings.getCategoryAttributes(category).stream().filter(value -> value.attributeId().equals(global.id()))
                .findFirst().orElseThrow().isRequired()).isFalse();
        long selectOption = select.options().getFirst().id();
        List<Long> multiOptions = multi.options().stream().map(com.yongtuo.site.attribute.AdminAttributeOptionDto::id).toList();
        ProductDto created = products.create(request(category, List.of(), List.of(new AdminProductVariantRequest("FOUR", "四类型", "Four", 0,
                ProductStatus.DRAFT, List.of(
                        new AdminProductVariantValueRequest(text.id(), "123,456", null, null, null),
                        new AdminProductVariantValueRequest(number.id(), null, null, new BigDecimal("8.25"), null),
                        new AdminProductVariantValueRequest(select.id(), null, null, null, selectOption),
                        new AdminProductVariantValueRequest(multi.id(), null, null, null, null, multiOptions))))));
        assertThat(created.variants().getFirst().values()).extracting(ProductAttributeDto::dataType)
                .containsExactly(AttributeDataType.TEXT, AttributeDataType.NUMBER, AttributeDataType.SELECT, AttributeDataType.MULTI_SELECT);
        assertThat(created.variants().getFirst().values().getFirst().valueZh()).isEqualTo("123,456");
        assertThat(created.variants().getFirst().values().getLast().optionIds())
                .containsExactlyElementsOf(multiOptions.stream().sorted().toList());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id = ? AND attribute_id = ?",
                Integer.class, created.variants().getFirst().id(), text.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id = ? AND attribute_id = ?",
                Integer.class, created.variants().getFirst().id(), number.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id = ? AND attribute_id = ?",
                Integer.class, created.variants().getFirst().id(), select.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_variant_value WHERE variant_id = ? AND attribute_id = ?",
                Integer.class, created.variants().getFirst().id(), multi.id())).isEqualTo(multiOptions.size());
        assertThat(jdbc.queryForList("SELECT value_key FROM product_variant_value WHERE variant_id = ? AND attribute_id = ? ORDER BY value_key",
                Long.class, created.variants().getFirst().id(), multi.id())).containsExactlyElementsOf(multiOptions.stream().sorted().toList());
    }

    @Test void normalizedValueKeyRejectsDuplicateNonMultiRow() {
        long category = category("synthetic-value-key-category");
        AdminAttributeDto text = attribute("value-key-text", AttributeDataType.TEXT, true, false, false, List.of());
        bindings.replaceCategoryBindings(category, List.of(binding(text, false, false, 0)));
        ProductDto created = products.create(request(category,
                List.of(new AdminProductAttributeRequest(text.id(), "one", null, null, null, List.of(), 0)),
                List.of(variant("VALUE-KEY-VARIANT", "variant", 0))));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO product_attribute_value(product_id, attribute_id, value_zh, value_key, sort_order)
                VALUES (?, ?, 'duplicate', 0, 0)
                """, created.id(), text.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        long variantId = created.variants().getFirst().id();
        jdbc.update("INSERT INTO product_variant_value(variant_id, attribute_id, value_zh, value_key) VALUES (?, ?, 'first', 0)",
                variantId, text.id());
        assertThatThrownBy(() -> jdbc.update("INSERT INTO product_variant_value(variant_id, attribute_id, value_zh, value_key) VALUES (?, ?, 'duplicate', 0)",
                variantId, text.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void nestedHttpValidationRejectsMalformedVariantBeforeWrite() throws Exception {
        mvc.perform(post("/api/v1/admin/products").with(user("synthetic-admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId":1,"productCode":"HTTP-TEST","slug":"http-test","nameZh":"测试",
                     "englishStatus":"CONFIRMED","isFeatured":false,"sortOrder":0,"status":"DRAFT",
                     "attributes":[],"variants":[{"variantCode":"","nameZh":"","status":"DRAFT","values":[{"attributeId":null}]}]}
                    """))
                .andExpect(status().isBadRequest());
    }

    private long category(String slug) {
        AdminCategoryDto dto = categories.create(new AdminCategoryWriteRequest(null, "测试分类", "Synthetic category", slug,
                null, null, null, CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE, false, null, null, null, null));
        return dto.id();
    }

    private AdminAttributeDto attribute(String code, AttributeDataType type, boolean global, boolean filterable,
                                        boolean required, List<AdminAttributeOptionRequest> options) {
        return definitions.create(new AdminAttributeWriteRequest("属性 " + code, "Attribute " + code, code, type, null,
                global, filterable, required, 0, AttributeStatus.ACTIVE, options));
    }

    private static CategoryAttributeBindingRequest binding(AdminAttributeDto a, boolean filterable, boolean required, int order) {
        return new CategoryAttributeBindingRequest(a.id(), filterable, required, true, order);
    }

    private static AdminAttributeOptionRequest option(String code) {
        return new AdminAttributeOptionRequest(null, code, code, code, 0, AttributeStatus.ACTIVE);
    }

    private static AdminProductCreateRequest request(long category, List<AdminProductAttributeRequest> attributes,
                                                     List<AdminProductVariantRequest> variants) {
        return new AdminProductCreateRequest(category, "SYN-BOLT-RED", "synthetic-bolt", "测试用螺栓（合成测试）", "Synthetic test bolt",
                null, null, null, null, EnglishStatus.CONFIRMED, null, false, 0, ProductStatus.DRAFT,
                null, null, null, null, attributes, variants);
    }

    private static AdminProductVariantRequest variant(String code, String name, int order) {
        return new AdminProductVariantRequest(code, name, name, order, ProductStatus.DRAFT, List.of());
    }

    private static AdminProductVariantRequest variantText(String code, String value, long attributeId) {
        return new AdminProductVariantRequest(code, code, code, 0, ProductStatus.DRAFT,
                List.of(new AdminProductVariantValueRequest(attributeId, value, null, null, null)));
    }

    private static AdminProductVariantRequest variant(String code, String name, int order, long attributeId, String numeric) {
        return new AdminProductVariantRequest(code, name, name, order, ProductStatus.DRAFT,
                List.of(new AdminProductVariantValueRequest(attributeId, null, null, new BigDecimal(numeric), null)));
    }

    private static AdminProductUpdateRequest updateRequest(ProductDto product, long category,
                                                           List<AdminProductAttributeRequest> attributes,
                                                           List<AdminProductVariantRequest> variants) {
        return new AdminProductUpdateRequest(category, product.productCode(), product.slug(), product.nameZh(), product.nameEn(),
                product.summaryZh(), product.summaryEn(), product.descriptionZh(), product.descriptionEn(),
                product.englishStatus(), product.coverImage(), product.isFeatured(), product.sortOrder(),
                product.seoTitleZh(), product.seoTitleEn(), product.seoDescriptionZh(), product.seoDescriptionEn(),
                product.status(), attributes, variants);
    }
}
