package com.yongtuo.site.product.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.yongtuo.site.YongtuoApiApplication;
import com.yongtuo.site.media.ObjectStorageUploadConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(classes = {
        YongtuoApiApplication.class,
        ObjectStorageUploadConfig.class,
        BatchImageService.class
})
@Testcontainers
class BatchImageDefaultStorageTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired BatchImageService service;

    @BeforeEach
    void seedProduct() {
        jdbc.update("DELETE FROM product_image");
        jdbc.update("DELETE FROM media_file");
        jdbc.update("DELETE FROM product");
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                SELECT id,'YT001','yt001','测试产品','DRAFT','EMPTY'
                FROM product_category WHERE slug='luoding'
                """);
    }

    @Test
    void matchedFileFailsSafelyWhenObjectStorageIsNotConfigured() {
        MockMultipartFile image = new MockMultipartFile("files", "YT001-1.jpg", "image/jpeg",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});

        BatchImageReport report = service.upload(List.of(image));

        assertThat(report.success()).isZero();
        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.files().getFirst().reason()).isEqualTo("STORAGE_UPLOAD_FAILED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_image", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_file", Integer.class)).isZero();
    }
}
