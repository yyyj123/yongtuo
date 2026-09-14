package com.yongtuo.site.site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.category.AdminCategoryDto;
import com.yongtuo.site.category.AdminCategoryWriteRequest;
import com.yongtuo.site.category.CategoryMode;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.category.CategoryStatus;
import com.yongtuo.site.product.AdminProductCreateRequest;
import com.yongtuo.site.product.AdminProductUpdateRequest;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductDto;
import com.yongtuo.site.product.ProductService;
import com.yongtuo.site.product.ProductStatus;
import java.util.List;
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
class UrlRedirectServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired UrlRedirectService redirects;
    @Autowired ProductService products;
    @Autowired CategoryService categories;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM url_redirect");
        jdbc.update("DELETE FROM product_attachment");
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_image");
        jdbc.update("DELETE FROM product");
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");
    }

    @Test
    void migrationMatchesRedirectContract() {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'url_redirect'
                ORDER BY ordinal_position
                """, String.class);
        assertThat(columns).containsExactly("id", "old_path", "new_path", "redirect_type", "created_at");
        assertThat(jdbc.queryForObject("""
                SELECT column_default FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'url_redirect'
                  AND column_name = 'redirect_type'
                """, Integer.class)).isEqualTo(301);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'url_redirect'
                  AND column_name = 'old_path' AND non_unique = 0
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void publishedProductSlugChangeCreatesBilingual301AndPublicApiResolvesIt() throws Exception {
        long categoryId = category("redirect-products", CategoryStatus.ACTIVE).id();
        ProductDto product = products.create(productRequest(categoryId, "YT-REDIRECT-001",
                "old-product", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        products.update(product.id(), productUpdate(categoryId, product.productCode(),
                "new-product", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        assertThat(redirects.resolve("/products/old-product"))
                .contains(new UrlRedirect("/products/old-product", "/products/new-product", 301));
        assertThat(redirects.resolve("/en/products/old-product"))
                .contains(new UrlRedirect("/en/products/old-product", "/en/products/new-product", 301));
        mvc.perform(get("/api/v1/public/redirects").param("path", "/products/old-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oldPath").value("/products/old-product"))
                .andExpect(jsonPath("$.data.newPath").value("/products/new-product"))
                .andExpect(jsonPath("$.data.redirectType").value(301));
        mvc.perform(get("/api/v1/public/redirects").param("path", "/products/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectsOnlyUrlsThatWereAndRemainPublic() {
        long categoryId = category("visibility-products", CategoryStatus.ACTIVE).id();
        ProductDto draft = products.create(productRequest(categoryId, "YT-REDIRECT-002",
                "draft-old", ProductStatus.DRAFT, EnglishStatus.CONFIRMED));
        products.update(draft.id(), productUpdate(categoryId, draft.productCode(),
                "draft-new", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        assertThat(redirects.resolve("/products/draft-old")).isEmpty();

        ProductDto chineseOnly = products.create(productRequest(categoryId, "YT-REDIRECT-003",
                "zh-old", ProductStatus.PUBLISHED, EnglishStatus.EMPTY));
        products.update(chineseOnly.id(), productUpdate(categoryId, chineseOnly.productCode(),
                "zh-new", ProductStatus.PUBLISHED, EnglishStatus.EMPTY));
        assertThat(redirects.resolve("/products/zh-old")).isPresent();
        assertThat(redirects.resolve("/en/products/zh-old")).isEmpty();

        ProductDto offline = products.create(productRequest(categoryId, "YT-REDIRECT-004",
                "offline-old", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        products.update(offline.id(), productUpdate(categoryId, offline.productCode(),
                "offline-new", ProductStatus.OFFLINE, EnglishStatus.CONFIRMED));
        assertThat(redirects.resolve("/products/offline-old")).isEmpty();
    }

    @Test
    void activeCategorySlugChangeCreatesCanonicalFilterRedirects() {
        AdminCategoryDto category = category("old-category", CategoryStatus.ACTIVE);

        categories.update(category.id(), categoryRequest("new-category", CategoryStatus.ACTIVE));

        assertThat(redirects.resolve("/products?category=old-category"))
                .contains(new UrlRedirect("/products?category=old-category",
                        "/products?category=new-category", 301));
        assertThat(redirects.resolve("/en/products?category=old-category"))
                .contains(new UrlRedirect("/en/products?category=old-category",
                        "/en/products?category=new-category", 301));
    }

    @Test
    void successiveProductSlugChangesFlattenAllHistoricalRedirects() {
        long categoryId = category("redirect-chain-products", CategoryStatus.ACTIVE).id();
        ProductDto product = products.create(productRequest(categoryId, "YT-REDIRECT-CHAIN",
                "chain-a", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        products.update(product.id(), productUpdate(categoryId, product.productCode(),
                "chain-b", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        products.update(product.id(), productUpdate(categoryId, product.productCode(),
                "chain-c", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        assertThat(redirects.resolve("/products/chain-a"))
                .contains(new UrlRedirect("/products/chain-a", "/products/chain-c", 301));
        assertThat(redirects.resolve("/products/chain-b"))
                .contains(new UrlRedirect("/products/chain-b", "/products/chain-c", 301));
        assertThat(redirects.resolve("/en/products/chain-a"))
                .contains(new UrlRedirect("/en/products/chain-a", "/en/products/chain-c", 301));
    }

    @Test
    void returningToHistoricalSlugRemovesStaleMappingInsteadOfCreatingALoop() {
        long categoryId = category("redirect-return-products", CategoryStatus.ACTIVE).id();
        ProductDto product = products.create(productRequest(categoryId, "YT-REDIRECT-RETURN",
                "return-a", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        products.update(product.id(), productUpdate(categoryId, product.productCode(),
                "return-b", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        products.update(product.id(), productUpdate(categoryId, product.productCode(),
                "return-a", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        assertThat(redirects.resolve("/products/return-a")).isEmpty();
        assertThat(redirects.resolve("/products/return-b"))
                .contains(new UrlRedirect("/products/return-b", "/products/return-a", 301));
        assertThat(redirects.resolve("/en/products/return-a")).isEmpty();
    }

    @Test
    void newlyPublishedEntityRemovesRedirectForItsReusedCanonicalSlug() {
        long categoryId = category("redirect-reuse-products", CategoryStatus.ACTIVE).id();
        ProductDto first = products.create(productRequest(categoryId, "YT-REDIRECT-REUSE-1",
                "reused-slug", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        products.update(first.id(), productUpdate(categoryId, first.productCode(),
                "moved-slug", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        assertThat(redirects.resolve("/products/reused-slug")).isPresent();

        products.create(productRequest(categoryId, "YT-REDIRECT-REUSE-2",
                "reused-slug", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));

        assertThat(redirects.resolve("/products/reused-slug")).isEmpty();
        assertThat(redirects.resolve("/en/products/reused-slug")).isEmpty();
    }

    @Test
    void redirectFailureRollsBackProductSlugUpdate() {
        long categoryId = category("rollback-products", CategoryStatus.ACTIVE).id();
        ProductDto product = products.create(productRequest(categoryId, "YT-REDIRECT-005",
                "rollback-old", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED));
        jdbc.execute("""
                CREATE TRIGGER reject_redirect BEFORE INSERT ON url_redirect
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'synthetic redirect failure'
                """);
        try {
            assertThatThrownBy(() -> products.update(product.id(), productUpdate(categoryId,
                    product.productCode(), "rollback-new", ProductStatus.PUBLISHED, EnglishStatus.CONFIRMED)))
                    .isInstanceOf(Exception.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_redirect");
        }

        assertThat(jdbc.queryForObject("SELECT slug FROM product WHERE id = ?", String.class, product.id()))
                .isEqualTo("rollback-old");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM url_redirect", Integer.class)).isZero();
    }

    @Test
    void secondCategoryRedirectFailureRollsBackSlugAndFirstRedirect() {
        AdminCategoryDto category = category("category-rollback-old", CategoryStatus.ACTIVE);
        jdbc.execute("""
                CREATE TRIGGER reject_english_category_redirect BEFORE INSERT ON url_redirect
                FOR EACH ROW BEGIN
                    IF NEW.old_path LIKE '/en/products?category=%' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'synthetic second redirect failure';
                    END IF;
                END
                """);
        try {
            assertThatThrownBy(() -> categories.update(category.id(),
                    categoryRequest("category-rollback-new", CategoryStatus.ACTIVE)))
                    .isInstanceOf(Exception.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_english_category_redirect");
        }

        assertThat(jdbc.queryForObject("SELECT slug FROM product_category WHERE id = ?",
                String.class, category.id())).isEqualTo("category-rollback-old");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM url_redirect", Integer.class)).isZero();
    }

    private AdminCategoryDto category(String slug, CategoryStatus status) {
        return categories.create(categoryRequest(slug, status));
    }

    private static AdminCategoryWriteRequest categoryRequest(String slug, CategoryStatus status) {
        return new AdminCategoryWriteRequest(null, "合成重定向分类", "Synthetic redirect category",
                slug, null, null, null, CategoryMode.NORMAL, 0, status, false,
                null, null, null, null);
    }

    private static AdminProductCreateRequest productRequest(long categoryId, String code, String slug,
                                                            ProductStatus status, EnglishStatus englishStatus) {
        return new AdminProductCreateRequest(categoryId, code, slug, "合成重定向产品", "Synthetic redirect product",
                null, null, null, null, englishStatus, null, false, 0, status,
                null, null, null, null, List.of(), List.of());
    }

    private static AdminProductUpdateRequest productUpdate(long categoryId, String code, String slug,
                                                           ProductStatus status, EnglishStatus englishStatus) {
        return new AdminProductUpdateRequest(categoryId, code, slug, "合成重定向产品", "Synthetic redirect product",
                null, null, null, null, englishStatus, null, false, 0,
                null, null, null, null, status, List.of(), List.of());
    }
}
