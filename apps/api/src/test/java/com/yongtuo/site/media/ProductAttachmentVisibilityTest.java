package com.yongtuo.site.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.product.query.ProductFilter;
import com.yongtuo.site.product.query.ProductQueryService;
import com.yongtuo.site.product.query.PublicProductAttachmentDto;
import com.yongtuo.site.product.query.PublicProductDto;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductAttachmentVisibilityTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired ProductQueryService products;
    @Autowired ObjectStorageService storage;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    long productId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product_attribute_value");
        jdbc.update("DELETE FROM product_attachment");
        jdbc.update("DELETE FROM product_image");
        jdbc.update("DELETE FROM media_file");
        jdbc.update("DELETE FROM category_attribute");
        jdbc.update("DELETE FROM attribute_option");
        jdbc.update("DELETE FROM attribute_definition");
        jdbc.update("DELETE FROM product");
        jdbc.update("UPDATE product_category SET parent_id=NULL");
        jdbc.update("DELETE FROM product_category");

        jdbc.update("INSERT INTO product_category(name_zh,name_en,slug,category_mode,status) VALUES ('测试分类','Synthetic category','attachment-test','NORMAL','ACTIVE')");
        long categoryId = jdbc.queryForObject("SELECT id FROM product_category WHERE slug='attachment-test'", Long.class);
        jdbc.update("INSERT INTO product(category_id,product_code,slug,name_zh,name_en,status,english_status) VALUES (?,?,?,?,?,'PUBLISHED','CONFIRMED')",
                categoryId, "TEST-ATT-1", "attachment-product", "附件测试产品", "Attachment Test Product");
        productId = jdbc.queryForObject("SELECT id FROM product WHERE product_code='TEST-ATT-1'", Long.class);

        attachment("private-hidden.pdf", "私密资料", "Private file",
                "https://raw-admin.invalid/private-hidden.pdf", false, false, 1, null);
        attachment("private-download.pdf", "私密但误开下载", "Private but downloadable",
                "https://raw-admin.invalid/private-download.pdf", false, true, 2, null);

        long viewMedia = media("internal/view-only.pdf", "https://cdn.example.test/view-only.pdf", "ACTIVE", false);
        attachment("view-only.pdf", "公开仅查看", "Public view only",
                "https://raw-admin.invalid/view-only.pdf", true, false, 3, viewMedia);
        long downloadMedia = media("internal/download.pdf", "https://cdn.example.test/download.pdf", "ACTIVE", false);
        attachment("download.pdf", "公开可下载", "Public download",
                "https://raw-admin.invalid/download.pdf", true, true, 4, downloadMedia);
        attachment("legacy.pdf", "公开旧附件", "Public legacy attachment",
                "https://raw-admin.invalid/legacy.pdf", true, true, 5, null);
        long inactiveMedia = media("internal/inactive.pdf", "https://cdn.example.test/inactive.pdf", "INACTIVE", false);
        attachment("inactive.pdf", "公开停用媒体", "Public inactive media",
                "https://raw-admin.invalid/inactive.pdf", true, true, 6, inactiveMedia);
        long deletedMedia = media("internal/deleted.pdf", "https://cdn.example.test/deleted.pdf", "ACTIVE", true);
        attachment("deleted.pdf", "公开已删媒体", "Public deleted media",
                "https://raw-admin.invalid/deleted.pdf", true, true, 7, deletedMedia);
    }

    @Test
    void appliesPublicAndDownloadPermissionMatrixWithoutRawFieldLeakage() {
        PublicProductDto dto = products.detail("attachment-product", Locale.CHINESE);

        assertThat(dto.publicAttachments()).extracting(PublicProductAttachmentDto::title)
                .containsExactly("公开仅查看", "公开可下载", "公开旧附件", "公开停用媒体", "公开已删媒体");
        assertThat(dto.publicAttachments().get(0).downloadUrl()).isNull();
        assertThat(dto.publicAttachments().get(1).downloadUrl()).isEqualTo("https://cdn.example.test/download.pdf");
        assertThat(dto.publicAttachments()).element(2).extracting(PublicProductAttachmentDto::downloadUrl).isNull();
        assertThat(dto.publicAttachments()).element(3).extracting(PublicProductAttachmentDto::downloadUrl).isNull();
        assertThat(dto.publicAttachments()).element(4).extracting(PublicProductAttachmentDto::downloadUrl).isNull();
        assertThat(dto.toString()).doesNotContain("private-hidden", "private-download", "raw-admin", "internal/");

        assertThat(products.list(new ProductFilter(null, Map.of(), null, null, null), Locale.CHINESE)
                .items().getFirst().publicAttachments()).isEmpty();
    }

    @Test
    void localizesPublicTitlesAndPublicJsonContainsNoAdministrativeAttachmentFields() throws Exception {
        assertThat(products.detail("attachment-product", Locale.ENGLISH).publicAttachments())
                .extracting(PublicProductAttachmentDto::title)
                .containsExactly("Public view only", "Public download", "Public legacy attachment",
                        "Public inactive media", "Public deleted media");

        String json = mvc.perform(get("/api/v1/public/products/attachment-product")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicAttachments.length()").value(5))
                .andExpect(jsonPath("$.data.publicAttachments[0].title").value("Public view only"))
                .andExpect(jsonPath("$.data.publicAttachments[0].downloadUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.publicAttachments[1].downloadUrl")
                        .value("https://cdn.example.test/download.pdf"))
                .andExpect(jsonPath("$.data.publicAttachments[2].downloadUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.publicAttachments[3].downloadUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.publicAttachments[4].downloadUrl").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain("private-hidden", "private-download", "raw-admin", "internal/",
                "storageKey", "mediaId", "fileUrl", "isPublic", "allowDownload");
    }

    @Test
    void storageAdaptersAreExplicitAndNeverCreateProductionFallbackUrls() {
        assertThat(storage).isInstanceOf(LocalTestObjectStorageService.class);
        assertThat(storage.resolveDownloadUrl("internal/download.pdf", "https://cdn.example.test/download.pdf"))
                .contains("https://cdn.example.test/download.pdf");
        assertThat(storage.resolveDownloadUrl("internal/download.pdf", "https:/missing-host.pdf")).isEmpty();
        assertThat(storage.resolveDownloadUrl("internal/download.pdf", "http:missing-authority.pdf")).isEmpty();
        assertThat(new DisabledObjectStorageService()
                .resolveDownloadUrl("internal/download.pdf", "https://cdn.example.test/download.pdf")).isEmpty();
    }

    private long media(String storageKey, String publicUrl, String status, boolean deleted) {
        jdbc.update("INSERT INTO media_file(storage_key,original_name,public_url,file_type,mime_type,file_size,status,deleted_at) VALUES (?,?,?,'DOCUMENT','application/pdf',100,?,?)",
                storageKey, storageKey.substring(storageKey.lastIndexOf('/') + 1), publicUrl, status,
                deleted ? java.time.LocalDateTime.now() : null);
        return jdbc.queryForObject("SELECT id FROM media_file WHERE storage_key=?", Long.class, storageKey);
    }

    private void attachment(String fileName, String zh, String en, String rawUrl,
                            boolean isPublic, boolean allowDownload, int sortOrder, Long mediaId) {
        jdbc.update("INSERT INTO product_attachment(product_id,media_id,file_name,display_name_zh,display_name_en,file_url,file_type,is_public,allow_download,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?)",
                productId, mediaId, fileName, zh, en, rawUrl, "application/pdf", isPublic, allowDownload, sortOrder);
    }
}
