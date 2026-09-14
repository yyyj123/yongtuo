package com.yongtuo.site.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.product.query.PublicProductDto;
import java.util.Locale;
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
class SearchServiceTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired SearchService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    long activeCategory;
    long standard;
    long material;
    long stainless;
    long brass;
    long bolt;

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

        activeCategory = category("紧固件", "Fasteners", "fasteners", "ACTIVE");
        long inactiveCategory = category("隐藏分类", "Hidden category", "hidden", "INACTIVE");
        standard = attribute("standard", "TEXT");
        material = attribute("material", "SELECT");
        bindVisible(standard, 1);
        bindVisible(material, 2);
        stainless = option(material, "stainless-steel", "不锈钢", "Stainless steel");
        brass = option(material, "brass", "黄铜", "Brass");
        bolt = product(activeCategory, "YT-HB-001", "hex-bolt", "六角螺栓", "Hex Bolt", "PUBLISHED", "CONFIRMED", 1);
        textValue(bolt, standard, "DIN 933", "DIN 933");
        optionValue(bolt, material, stainless);
        long publishedVariant = variant(bolt, "HB-M8", "M8 型号", "M8 Model", "PUBLISHED");
        variantOptionValue(publishedVariant, material, brass);
        product(activeCategory, "YT-DRAFT", "draft", "草稿针", "Draft Pin", "DRAFT", "CONFIRMED", 2);
        product(activeCategory, "YT-EMPTY-EN", "empty-en", "中文可见", "Secret English", "PUBLISHED", "EMPTY", 3);
        product(inactiveCategory, "YT-HIDDEN", "hidden-product", "隐藏产品", "Hidden Product", "PUBLISHED", "CONFIRMED", 4);
        long deleted = product(activeCategory, "YT-DELETED", "deleted", "删除产品", "Deleted Product", "PUBLISHED", "CONFIRMED", 5);
        jdbc.update("UPDATE product SET deleted_at=CURRENT_TIMESTAMP WHERE id=?", deleted);
    }

    @Test
    void searchesChineseAndEnglishNamesAndProductCode() {
        assertSlugs("六角", Locale.CHINESE, "hex-bolt");
        assertSlugs("Hex", Locale.CHINESE, "hex-bolt");
        assertSlugs("YT-HB-001", Locale.CHINESE, "hex-bolt");
    }

    @Test
    void searchesPublishedVariantMaterialStandardAndCategoryAsProducts() {
        assertSlugs("HB-M8", Locale.CHINESE, "hex-bolt");
        assertSlugs("Stainless", Locale.CHINESE, "hex-bolt");
        assertSlugs("Brass", Locale.CHINESE, "hex-bolt");
        assertSlugs("DIN 933", Locale.CHINESE, "hex-bolt");
        assertSlugs("紧固件", Locale.CHINESE, "hex-bolt", "empty-en");
        assertSlugs("fasteners", Locale.CHINESE, "hex-bolt", "empty-en");
        variant(bolt, "SECRET-VARIANT", "草稿型号", "Draft model", "DRAFT");
        assertThat(service.search("SECRET-VARIANT", Locale.CHINESE).items()).isEmpty();
    }

    @Test
    void emptyQueriesReturnNoResultsOrSuggestions() {
        assertThat(service.search("  ", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("", Locale.CHINESE).total()).isZero();
        assertThat(service.suggestions("  ", Locale.CHINESE)).isEmpty();
    }

    @Test
    void publicVisibilityAndEnglishConfirmationApplyToSearch() {
        assertThat(service.search("草稿针", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("隐藏产品", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("删除产品", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Secret English", Locale.ENGLISH).items()).isEmpty();
        assertThat(service.suggestions("草稿针", Locale.CHINESE)).isEmpty();
        assertThat(service.suggestions("隐藏产品", Locale.CHINESE)).isEmpty();
        assertThat(service.suggestions("Secret English", Locale.ENGLISH)).isEmpty();
        assertSlugs("Hex", Locale.ENGLISH, "hex-bolt");
    }

    @Test
    void treatsInjectionShapedAndWildcardInputAsBoundLiteralText() {
        assertThat(service.search("%' OR 1=1 --", Locale.CHINESE).items()).isEmpty();
        assertThat(service.suggestions("_' OR '1'='1", Locale.CHINESE)).isEmpty();
        assertThat(service.search("%", Locale.CHINESE).items()).isEmpty();
    }

    @Test
    void ignoresResidualProductAndVariantValuesForAnUnboundNonGlobalAttribute() {
        jdbc.update("UPDATE attribute_definition SET is_global=0 WHERE id=?", material);
        jdbc.update("DELETE FROM category_attribute WHERE category_id=? AND attribute_id=?",
                activeCategory, material);

        assertThat(service.search("Stainless", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Brass", Locale.CHINESE).items()).isEmpty();
        assertThat(service.suggestions("Brass", Locale.CHINESE)).isEmpty();
    }

    @Test
    void searchesOnlyEffectiveDynamicValuesMarkedForPublicDetail() {
        jdbc.update("DELETE FROM category_attribute WHERE category_id=? AND attribute_id=?",
                activeCategory, material);
        assertThat(service.search("Stainless", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Brass", Locale.CHINESE).items()).isEmpty();

        bindVisible(material, 2);
        jdbc.update("UPDATE category_attribute SET show_in_detail=0 WHERE category_id=? AND attribute_id=?",
                activeCategory, material);
        assertThat(service.search("Stainless", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Brass", Locale.CHINESE).items()).isEmpty();

        jdbc.update("UPDATE category_attribute SET show_in_detail=1, is_filterable=0 WHERE category_id=? AND attribute_id=?",
                activeCategory, material);
        assertSlugs("Stainless", Locale.CHINESE, "hex-bolt");
        assertSlugs("Brass", Locale.CHINESE, "hex-bolt");
    }

    @Test
    void ignoresInactiveDefinitionsAndOptionsOnProductAndVariantPaths() {
        jdbc.update("UPDATE attribute_definition SET status='INACTIVE' WHERE id=?", material);
        assertThat(service.search("Stainless", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Brass", Locale.CHINESE).items()).isEmpty();

        jdbc.update("UPDATE attribute_definition SET status='ACTIVE' WHERE id=?", material);
        jdbc.update("UPDATE attribute_option SET status='INACTIVE' WHERE id IN (?,?)", stainless, brass);
        assertThat(service.search("Stainless", Locale.CHINESE).items()).isEmpty();
        assertThat(service.search("Brass", Locale.CHINESE).items()).isEmpty();
    }

    @Test
    void suggestionsAreLocalizedProductFirstAndLimitedToTen() {
        for (int i = 0; i < 12; i++) {
            product(activeCategory, "YT-S-" + i, "suggestion-" + i, "搜索建议" + i,
                    "Search suggestion " + i, "PUBLISHED", "CONFIRMED", 10 + i);
        }
        assertThat(service.suggestions("搜索建议", Locale.CHINESE)).hasSize(10)
                .allMatch(item -> item.label().startsWith("搜索建议"));
        assertThat(service.suggestions("Search suggestion", Locale.ENGLISH)).hasSize(10)
                .allMatch(item -> item.label().startsWith("Search suggestion"));
    }

    @Test
    void publicRoutesExposeSearchAndSuggestions() throws Exception {
        mvc.perform(get("/api/v1/public/search").param("keyword", "DIN 933"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].slug").value("hex-bolt"));
        mvc.perform(get("/api/v1/public/search/suggestions").param("q", "Hex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("hex-bolt"));
        mvc.perform(get("/api/v1/public/search").param("keyword", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
    }

    private void assertSlugs(String keyword, Locale locale, String... slugs) {
        assertThat(service.search(keyword, locale).items()).extracting(PublicProductDto::slug).containsExactly(slugs);
    }

    private long category(String zh, String en, String slug, String status) {
        jdbc.update("INSERT INTO product_category(name_zh,name_en,slug,category_mode,status) VALUES (?,?,?,'NORMAL',?)",
                zh, en, slug, status);
        return jdbc.queryForObject("SELECT id FROM product_category WHERE slug=?", Long.class, slug);
    }

    private long attribute(String code, String type) {
        jdbc.update("INSERT INTO attribute_definition(name_zh,name_en,code,data_type,is_global,status) VALUES (?,?,?,?,1,'ACTIVE')",
                "测试参数", "Synthetic attribute", code, type);
        return jdbc.queryForObject("SELECT id FROM attribute_definition WHERE code=?", Long.class, code);
    }

    private long option(long attributeId, String code, String zh, String en) {
        jdbc.update("INSERT INTO attribute_option(attribute_id,value_code,label_zh,label_en,status) VALUES (?,?,?,?, 'ACTIVE')",
                attributeId, code, zh, en);
        return jdbc.queryForObject("SELECT id FROM attribute_option WHERE attribute_id=? AND value_code=?", Long.class, attributeId, code);
    }

    private void bindVisible(long attributeId, int sortOrder) {
        jdbc.update("INSERT INTO category_attribute(category_id,attribute_id,is_filterable,is_required,show_in_detail,sort_order) VALUES (?,?,1,0,1,?)",
                activeCategory, attributeId, sortOrder);
    }

    private long product(long categoryId, String code, String slug, String zh, String en,
                         String status, String englishStatus, int sort) {
        jdbc.update("INSERT INTO product(category_id,product_code,slug,name_zh,name_en,status,english_status,sort_order) VALUES (?,?,?,?,?,?,?,?)",
                categoryId, code, slug, zh, en, status, englishStatus, sort);
        return jdbc.queryForObject("SELECT id FROM product WHERE product_code=?", Long.class, code);
    }

    private void textValue(long productId, long attributeId, String zh, String en) {
        jdbc.update("INSERT INTO product_attribute_value(product_id,attribute_id,value_zh,value_en) VALUES (?,?,?,?)",
                productId, attributeId, zh, en);
    }

    private void optionValue(long productId, long attributeId, long optionId) {
        jdbc.update("INSERT INTO product_attribute_value(product_id,attribute_id,option_id,value_key) VALUES (?,?,?,?)",
                productId, attributeId, optionId, optionId);
    }

    private long variant(long productId, String code, String zh, String en, String status) {
        jdbc.update("INSERT INTO product_variant(product_id,variant_code,name_zh,name_en,status) VALUES (?,?,?,?,?)",
                productId, code, zh, en, status);
        return jdbc.queryForObject("SELECT id FROM product_variant WHERE product_id=? AND variant_code=?",
                Long.class, productId, code);
    }

    private void variantOptionValue(long variantId, long attributeId, long optionId) {
        jdbc.update("INSERT INTO product_variant_value(variant_id,attribute_id,option_id,value_key) VALUES (?,?,?,?)",
                variantId, attributeId, optionId, optionId);
    }
}
