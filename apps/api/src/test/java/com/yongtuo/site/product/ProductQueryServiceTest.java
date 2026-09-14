package com.yongtuo.site.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.product.query.ProductFilter;
import com.yongtuo.site.product.query.ProductQueryException;
import com.yongtuo.site.product.query.ProductQueryService;
import com.yongtuo.site.product.query.PublicProductDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class ProductQueryServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired ProductQueryService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    long fasteners;
    long machining;
    long material;
    long diameter;
    long steel;
    long brass;
    long localFinish;
    long coating;
    long zinc;
    long black;

    @Test void publicFiltersExposeOnlyEffectiveFilterableDefinitions() throws Exception {
        mvc.perform(get("/api/v1/public/product-filters").param("category", "fasteners"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.code == 'material')]").isNotEmpty())
            .andExpect(jsonPath("$.data[?(@.code == 'internal_note')]").isEmpty())
            .andExpect(jsonPath("$.data[?(@.code == 'local_finish')]").isEmpty());
    }
    @Test void keywordCombinesWithCategoryAndAttributesAndCanPaginate() throws Exception {
        mvc.perform(get("/api/v1/public/products").param("keyword", "brass-10").param("category", "fasteners").param("attr.material", "steel"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/public/products").param("keyword", "steel").param("pageSize", "1").param("page", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(2)).andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test void detailExposesOrderedImagesAndConfirmedSecondaryNameOnly() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM product WHERE slug = 'steel-10'", Long.class);
        jdbc.update("INSERT INTO product_image(product_id,image_url,alt_zh,alt_en,sort_order,is_cover) VALUES (?, '/test.jpg', 'test image', 'Test image', 0, 1)", id);
        mvc.perform(get("/api/v1/public/products/steel-10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.images[0].url").value("/test.jpg"))
            .andExpect(jsonPath("$.data.englishAvailable").value(true));
        jdbc.update("DELETE FROM product_image WHERE product_id = ?", id);
    }

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_attribute_value");
        jdbc.update("DELETE FROM category_attribute");
        jdbc.update("DELETE FROM attribute_option");
        jdbc.update("DELETE FROM attribute_definition");
        jdbc.update("DELETE FROM product");
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");

        fasteners = category("fasteners", "ACTIVE");
        machining = category("machining", "ACTIVE");
        long hidden = category("hidden", "INACTIVE");
        material = attribute("material", "SELECT", true, true, "ACTIVE");
        diameter = attribute("diameter", "NUMBER", true, true, "ACTIVE");
        attribute("internal_note", "TEXT", true, false, "ACTIVE");
        attribute("retired", "TEXT", true, true, "INACTIVE");
        localFinish = attribute("local_finish", "TEXT", false, false, "ACTIVE");
        bind(machining, localFinish, true, true);
        bind(fasteners, material, true, true);
        bind(fasteners, diameter, true, false);
        coating = attribute("coating", "MULTI_SELECT", true, true, "ACTIVE");
        bind(fasteners, coating, true, true);
        steel = option(material, "steel");
        brass = option(material, "brass");
        zinc = option(coating, "zinc");
        black = option(coating, "black");

        long steelTen = product(fasteners, "steel-10", "PUBLISHED", "CONFIRMED", 1);
        value(steelTen, material, null, steel);
        value(steelTen, diameter, new BigDecimal("10"), null);
        textValue(steelTen, localFinish, "polished");
        value(steelTen, coating, null, zinc);
        value(steelTen, coating, null, black);
        long brassTen = product(fasteners, "brass-10", "PUBLISHED", "CONFIRMED", 2);
        value(brassTen, material, null, brass);
        value(brassTen, diameter, new BigDecimal("10"), null);
        long steelTwelve = product(machining, "steel-12", "PUBLISHED", "CONFIRMED", 3);
        value(steelTwelve, material, null, steel);
        value(steelTwelve, diameter, new BigDecimal("12"), null);
        textValue(steelTwelve, localFinish, "polished");
        product(fasteners, "draft-product", "DRAFT", "CONFIRMED", 4);
        product(hidden, "hidden-category-product", "PUBLISHED", "CONFIRMED", 5);
        long deleted = product(fasteners, "deleted-product", "PUBLISHED", "CONFIRMED", 6);
        jdbc.update("UPDATE product SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", deleted);
    }

    @Test
    void filtersByCategorySelectNumericAndCombinesConditionsWithAnd() {
        assertThat(service.list(filter("fasteners", Map.of()), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-10", "brass-10");
        assertThat(service.list(filter(null, Map.of("material", List.of("steel"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-10", "steel-12");
        assertThat(service.list(filter(null, Map.of("diameter", List.of("10"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-10", "brass-10");
        assertThat(service.list(filter("fasteners", Map.of(
                "material", List.of("steel"), "diameter", List.of("10"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-10");
    }

    @Test
    void usesOrWithinOneAttributeAndAndBetweenDifferentAttributes() {
        ProductFilter filter = new ProductFilter(null, Map.of(
                "material", List.of("steel", "brass"), "diameter", List.of("10")), 1, 24, null);
        assertThat(service.list(filter, Locale.CHINESE).items()).extracting(PublicProductDto::slug)
                .containsExactly("steel-10", "brass-10");
    }

    @Test
    void categoryBoundFilterOnlyAppliesToProductsInBoundCategory() {
        assertThat(service.list(filter(null, Map.of("local_finish", List.of("polished"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-12");
    }

    @Test
    void effectiveFilterabilityUsesExplicitOverrideAndNeverGlobalizesNonGlobalDefinition() {
        long globallyFilterable = attribute("global_override", "TEXT", true, true, "ACTIVE");
        bind(fasteners, globallyFilterable, false, true);
        textValue(productId("steel-10"), globallyFilterable, "yes");
        textValue(productId("steel-12"), globallyFilterable, "yes");
        assertThat(service.list(filter(null, Map.of("global_override", List.of("yes"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-12");

        long nonGlobalDefault = attribute("non_global_default", "TEXT", false, true, "ACTIVE");
        bind(machining, nonGlobalDefault, true, true);
        textValue(productId("steel-10"), nonGlobalDefault, "yes");
        textValue(productId("steel-12"), nonGlobalDefault, "yes");
        assertThat(service.list(filter(null, Map.of("non_global_default", List.of("yes"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-12");
    }

    @Test
    void filtersRealMultiSelectRowsByStableOptionCode() {
        assertThat(service.list(filter(null, Map.of("coating", List.of("black"))), Locale.CHINESE).items())
                .extracting(PublicProductDto::slug).containsExactly("steel-10");
    }

    @Test
    void defaultsPageSizeTo24CapsAt100AndReturnsStablePageMetadata() {
        assertThat(new ProductFilter(null, Map.of(), 1, null, null).pageSize()).isEqualTo(24);
        assertThatThrownBy(() -> new ProductFilter(null, Map.of(), 1, 101, null))
                .isInstanceOf(ProductQueryException.class).extracting("code").isEqualTo(22001);
        var page = service.list(new ProductFilter(null, Map.of(), 2, 1, null), Locale.CHINESE);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(1);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.items()).extracting(PublicProductDto::slug).containsExactly("brass-10");
    }

    @Test
    void rejectsUnknownNonFilterableInactiveAttributesAndSorts() {
        for (String code : List.of("unknown", "internal_note", "retired")) {
            assertThatThrownBy(() -> service.list(filter(null, Map.of(code, List.of("x"))), Locale.CHINESE))
                    .isInstanceOf(ProductQueryException.class).extracting("code").isEqualTo(22001);
        }
        assertThatThrownBy(() -> service.list(new ProductFilter(null, Map.of(), 1, 24, "name; DROP TABLE product"), Locale.CHINESE))
                .isInstanceOf(ProductQueryException.class).extracting("code").isEqualTo(22001);
    }

    @Test
    void sqlInjectionShapedFilterInputsRemainBoundData() {
        assertThat(service.list(filter("fasteners' OR 1=1 --", Map.of()), Locale.CHINESE).items()).isEmpty();
        assertThat(service.list(filter(null, Map.of("material", List.of("steel') OR 1=1 --"))), Locale.CHINESE).items())
                .isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class)).isEqualTo(6);
    }

    @Test
    void publicDetailUsesLocalizedPublicCompositionAndHidesAdminFieldsAndNonPublicContent() throws Exception {
        long publishedVariant = variant(productId("steel-10"), "PUB-V", "公开规格", "Public variant", "PUBLISHED", 1);
        variantValue(publishedVariant, material, steel);
        variant(productId("steel-10"), "DRAFT-V", "草稿规格", "Draft variant", "DRAFT", 2);
        variant(productId("steel-10"), "OFFLINE-V", "下架规格", "Offline variant", "OFFLINE", 3);
        assertThat(service.detail("steel-10", Locale.CHINESE).name()).isEqualTo("测试steel-10");
        assertThat(service.detail("steel-10", Locale.ENGLISH).name()).isEqualTo("Synthetic steel-10");
        assertThat(service.detail("steel-10", Locale.CHINESE).attributes())
                .extracting("code").containsExactly("coating", "material");
        assertThat(service.detail("steel-10", Locale.CHINESE).attributes().getFirst().name()).isEqualTo("测试参数");
        assertThat(service.detail("steel-10", Locale.ENGLISH).attributes().getFirst().name()).isEqualTo("Synthetic attribute");
        assertThat(service.detail("steel-10", Locale.CHINESE).attributes().getFirst().options())
                .extracting("code").containsExactly("zinc", "black");
        assertThat(service.detail("steel-10", Locale.CHINESE).attributes().getFirst().options().getFirst().label())
                .isEqualTo("zinc");
        assertThat(service.detail("steel-10", Locale.ENGLISH).attributes().getFirst().options().getFirst().label())
                .isEqualTo("Synthetic zinc");
        assertThat(service.detail("steel-10", Locale.CHINESE).variants())
                .extracting("variantCode").containsExactly("PUB-V");
        assertThat(service.detail("steel-10", Locale.CHINESE).variants().getFirst().name()).isEqualTo("公开规格");
        assertThat(service.detail("steel-10", Locale.ENGLISH).variants().getFirst().name()).isEqualTo("Public variant");

        mvc.perform(get("/api/v1/public/products/steel-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.categoryId").doesNotExist())
                .andExpect(jsonPath("$.data.nameZh").doesNotExist())
                .andExpect(jsonPath("$.data.nameEn").doesNotExist())
                .andExpect(jsonPath("$.data.attributes[0].attributeId").doesNotExist())
                .andExpect(jsonPath("$.data.attributes[0].nameZh").doesNotExist())
                .andExpect(jsonPath("$.data.attributes[0].optionId").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].status").doesNotExist());
        jdbc.update("UPDATE product SET english_status = 'EMPTY' WHERE slug = 'steel-10'");
        assertThatThrownBy(() -> service.detail("steel-10", Locale.ENGLISH))
                .isInstanceOf(ProductQueryException.class).extracting("status.value").isEqualTo(404);
        assertThatThrownBy(() -> service.detail("draft-product", Locale.CHINESE))
                .isInstanceOf(ProductQueryException.class).extracting("status.value").isEqualTo(404);
        assertThatThrownBy(() -> service.detail("hidden-category-product", Locale.CHINESE))
                .isInstanceOf(ProductQueryException.class).extracting("status.value").isEqualTo(404);
        assertThatThrownBy(() -> service.detail("deleted-product", Locale.CHINESE))
                .isInstanceOf(ProductQueryException.class).extracting("status.value").isEqualTo(404);
    }

    @Test
    void publicRoutesExposeFiltersPaginationAndBusiness404() throws Exception {
        mvc.perform(get("/api/v1/public/products")
                        .param("category", "fasteners").param("attr.material", "steel")
                        .param("attr.diameter", "10").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].slug").value("steel-10"))
                .andExpect(jsonPath("$.data.pageSize").value(1));
        mvc.perform(get("/api/v1/public/products/steel-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attributes.length()").value(2));
        mvc.perform(get("/api/v1/public/products/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(22002));
        mvc.perform(get("/api/v1/public/products").param("pageSize", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(22001));
    }

    private static ProductFilter filter(String category, Map<String, List<String>> attributes) {
        return new ProductFilter(category, attributes, 1, 24, null);
    }

    private long category(String slug, String status) {
        jdbc.update("INSERT INTO product_category(name_zh, slug, category_mode, status) VALUES (?, ?, 'NORMAL', ?)",
                "测试分类", slug, status);
        return jdbc.queryForObject("SELECT id FROM product_category WHERE slug = ?", Long.class, slug);
    }

    private long attribute(String code, String type, boolean global, boolean filterable, String status) {
        jdbc.update("INSERT INTO attribute_definition(name_zh, name_en, code, data_type, is_global, default_filterable, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "测试参数", "Synthetic attribute", code, type, global, filterable, status);
        return jdbc.queryForObject("SELECT id FROM attribute_definition WHERE code = ?", Long.class, code);
    }

    private void bind(long categoryId, long attributeId, boolean filterable, boolean detail) {
        jdbc.update("INSERT INTO category_attribute(category_id, attribute_id, is_filterable, show_in_detail) VALUES (?, ?, ?, ?)",
                categoryId, attributeId, filterable, detail);
    }

    private long productId(String slug) {
        return jdbc.queryForObject("SELECT id FROM product WHERE slug = ?", Long.class, slug);
    }

    private long variant(long productId, String code, String zh, String en, String status, int order) {
        jdbc.update("INSERT INTO product_variant(product_id, variant_code, name_zh, name_en, status, sort_order) VALUES (?, ?, ?, ?, ?, ?)",
                productId, code, zh, en, status, order);
        return jdbc.queryForObject("SELECT id FROM product_variant WHERE product_id = ? AND variant_code = ?", Long.class, productId, code);
    }

    private void variantValue(long variantId, long attributeId, long optionId) {
        jdbc.update("INSERT INTO product_variant_value(variant_id, attribute_id, option_id, value_key) VALUES (?, ?, ?, ?)",
                variantId, attributeId, optionId, optionId);
    }

    private long option(long attributeId, String code) {
        jdbc.update("INSERT INTO attribute_option(attribute_id, value_code, label_zh, label_en) VALUES (?, ?, ?, ?)",
                attributeId, code, code, "Synthetic " + code);
        return jdbc.queryForObject("SELECT id FROM attribute_option WHERE attribute_id = ? AND value_code = ?", Long.class, attributeId, code);
    }

    private long product(long categoryId, String slug, String status, String englishStatus, int sort) {
        jdbc.update("INSERT INTO product(category_id, product_code, name_zh, name_en, slug, english_status, sort_order, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                categoryId, "SYN-" + slug, "测试" + slug, "Synthetic " + slug, slug, englishStatus, sort, status);
        return jdbc.queryForObject("SELECT id FROM product WHERE slug = ?", Long.class, slug);
    }

    private void value(long productId, long attributeId, BigDecimal number, Long optionId) {
        jdbc.update("INSERT INTO product_attribute_value(product_id, attribute_id, numeric_value, option_id, value_key) VALUES (?, ?, ?, ?, ?)",
                productId, attributeId, number, optionId, optionId == null ? 0 : optionId);
    }

    private void textValue(long productId, long attributeId, String value) {
        jdbc.update("INSERT INTO product_attribute_value(product_id, attribute_id, value_zh, value_en) VALUES (?, ?, ?, ?)",
                productId, attributeId, value, value);
    }
}
