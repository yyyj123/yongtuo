package com.yongtuo.site.product.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductImportPreviewTest {

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
    @Autowired MockMvc mvc;
    @Autowired Environment environment;

    @BeforeEach
    void seed() {
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
        long categoryId = jdbc.queryForObject(
                "SELECT id FROM product_category WHERE slug='fasteners'", Long.class);
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                VALUES (?,?,?,?, 'DRAFT','EMPTY')
                """, categoryId, "YT-EXISTING", "existing-product", "已有产品");
        store.clear();
    }

    @Test
    void reportsDuplicateUnknownCategoryInvalidStatusAndMissingRequiredFieldWithoutWritingProducts()
            throws Exception {
        int before = productCount();
        MockMultipartFile file = workbook(List.of(
                row("YT-VALID", "valid-product", "fasteners", "有效产品", "DRAFT"),
                row("YT-DUP", "duplicate-one", "fasteners", "重复一", "DRAFT"),
                row("yt-dup", "duplicate-two", "fasteners", "重复二", "DRAFT"),
                row("YT-CATEGORY", "unknown-category", "missing", "未知分类", "DRAFT"),
                row("YT-STATUS", "invalid-status", "fasteners", "非法状态", "VISIBLE"),
                row("YT-EMPTY", "empty-required", "fasteners", "", "DRAFT"),
                row("YT-EXISTING", "existing-code", "fasteners", "编号已存在", "DRAFT")));

        ProductImportPreview preview = service.preview(file);

        assertThat(preview.importToken()).isNotBlank().hasSizeGreaterThanOrEqualTo(32);
        assertThat(preview.total()).isEqualTo(7);
        assertThat(preview.valid()).isEqualTo(1);
        assertThat(preview.warning()).isZero();
        assertThat(preview.error()).isEqualTo(6);
        assertThat(preview.rows()).extracting(ProductImportRowResult::rowNumber)
                .containsExactly(2, 3, 4, 5, 6, 7, 8);
        assertThat(preview.rows().get(1).messages()).contains("DUPLICATE_PRODUCT_CODE");
        assertThat(preview.rows().get(2).messages()).contains("DUPLICATE_PRODUCT_CODE");
        assertThat(preview.rows().get(3).messages()).contains("UNKNOWN_CATEGORY");
        assertThat(preview.rows().get(4).messages()).contains("INVALID_STATUS");
        assertThat(preview.rows().get(5).messages()).contains("REQUIRED_NAME_ZH");
        assertThat(preview.rows().get(6).messages()).contains("PRODUCT_CODE_EXISTS");
        assertThat(productCount()).isEqualTo(before);

        assertThatThrownBy(() -> preview.rows().add(preview.rows().getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        ImportPreviewStore.Snapshot snapshot = store.find(preview.importToken()).orElseThrow();
        assertThat(snapshot.rows()).hasSize(1);
        assertThat(snapshot.confirmable()).isFalse();
        assertThat(snapshot.rows().getFirst().productCode()).isEqualTo("YT-VALID");
        assertThatThrownBy(() -> snapshot.rows().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void adminEndpointRequiresAuthenticationAndReturnsPreviewContract() throws Exception {
        MockMultipartFile file = workbook(List.<String[]>of(
                row("YT-ROUTE", "route-product", "fasteners", "路由产品", "PUBLISHED")));

        mvc.perform(multipart("/api/v1/admin/products/import/preview").file(file))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/admin/products/import/preview").file(file)
                        .with(user("synthetic-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importToken").isNotEmpty())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.valid").value(1))
                .andExpect(jsonPath("$.data.warning").value(0))
                .andExpect(jsonPath("$.data.error").value(0))
                .andExpect(jsonPath("$.data.rows[0].status").value("PUBLISHED"));
        assertThat(productCount()).isEqualTo(1);
    }

    @Test
    void rejectsWrongFileTypeOversizedUploadAndFormulaCells() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "products.csv", "text/csv", "a,b".getBytes());
        assertThatThrownBy(() -> service.preview(csv))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23001);

        MockMultipartFile oversized = new MockMultipartFile(
                "file", "products.xlsx", XLSX, new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.preview(oversized))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23002);
        mvc.perform(multipart("/api/v1/admin/products/import/preview").file(oversized)
                        .with(user("synthetic-admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(23002));

        MockMultipartFile formula = formulaWorkbook();
        ProductImportPreview preview = service.preview(formula);
        assertThat(preview.error()).isEqualTo(1);
        assertThat(preview.rows().getFirst().messages()).contains("FORMULA_NOT_ALLOWED");
        assertThat(productCount()).isEqualTo(1);
    }

    @Test
    void usesDatabaseCollationForExistingAndInFileCodesAndCategoryResolution() throws Exception {
        long categoryId = jdbc.queryForObject(
                "SELECT id FROM product_category WHERE slug='fasteners'", Long.class);
        jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status)
                VALUES (?,?,?,?, 'DRAFT','EMPTY')
                """, categoryId, "YT-CAFE", "database-cafe", "排序规则产品");
        MockMultipartFile file = workbook(List.of(
                row("YT-CAFÉ", "accent-existing", "fasténers", "现有冲突", "DRAFT"),
                row("YT-INFILE-CAFE", "accent-file-one", "fasteners", "文件冲突一", "DRAFT"),
                row("YT-INFILE-CAFÉ", "accent-file-two", "fasteners", "文件冲突二", "DRAFT")));

        ProductImportPreview preview = service.preview(file);

        assertThat(preview.error()).isEqualTo(3);
        assertThat(preview.rows().getFirst().messages())
                .contains("PRODUCT_CODE_EXISTS")
                .doesNotContain("UNKNOWN_CATEGORY");
        assertThat(preview.rows().get(1).messages()).contains("DUPLICATE_PRODUCT_CODE");
        assertThat(preview.rows().get(2).messages()).contains("DUPLICATE_PRODUCT_CODE");
        assertThat(productCount()).isEqualTo(2);
    }

    @Test
    void rejectsEmptyMissingOrDuplicateHeadersAndMoreThanOneThousandRows() throws Exception {
        assertThatThrownBy(() -> service.preview(workbook(List.of())))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23003);
        assertThatThrownBy(() -> service.preview(workbook(
                List.of("productCode", "slug", "categorySlug", "nameZh"), List.of())))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23003);
        assertThatThrownBy(() -> service.preview(workbook(
                List.of("productCode", "slug", "categorySlug", "nameZh", "productCode"), List.of())))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23003);

        List<String[]> rows = IntStream.rangeClosed(1, 1001)
                .mapToObj(index -> row("YT-LIMIT-" + index, "limit-" + index,
                        "fasteners", "行数测试" + index, "DRAFT"))
                .toList();
        assertThatThrownBy(() -> service.preview(workbook(rows)))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23004);
    }

    @Test
    void rejectsZipWithTooManyEntriesBeforePoiBuildsWorkbook() throws Exception {
        assertThatThrownBy(() -> service.preview(xlsxWithExtraEntries(101)))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23001);
    }

    @Test
    void rejectsZipWhoseCombinedUncompressedContentExceedsTwentyMegabytes() throws Exception {
        int before = productCount();
        MockMultipartFile compressedBomb = xlsxWithLargeExtraEntries(3, 7 * 1024 * 1024);
        assertThat(compressedBomb.getSize()).isLessThan(5L * 1024 * 1024);

        assertThatThrownBy(() -> service.preview(compressedBomb))
                .isInstanceOf(ProductImportException.class)
                .extracting("code").isEqualTo(23001);
        assertThat(productCount()).isEqualTo(before);
    }

    @Test
    void countsUnicodeCodePointsWhenEnforcingDatabaseCharacterLimits() throws Exception {
        String twoHundredSupplementaryCharacters = "\uD83D\uDD29".repeat(200);

        ProductImportPreview preview = service.preview(workbook(List.<String[]>of(
                row("YT-UNICODE", "unicode-name", "fasteners",
                        twoHundredSupplementaryCharacters, "DRAFT"))));

        assertThat(preview.valid()).isEqualTo(1);
        assertThat(preview.error()).isZero();
    }

    @Test
    void applicationMultipartCeilingDoesNotLimitBatchRequestsToSixMegabytes() {
        assertThat(environment.getProperty("spring.servlet.multipart.max-file-size"))
                .isEqualTo("20MB");
        assertThat(environment.getProperty("spring.servlet.multipart.max-request-size"))
                .isEqualTo("256MB");
    }

    @Test
    void previewStoreEvictsOldestSnapshotAtItsBound() {
        NormalizedProductRow row = new NormalizedProductRow(
                2, 1, "YT-STORE", "store-row", "存储行", com.yongtuo.site.product.ProductStatus.DRAFT);
        String first = store.put(List.of(row), true);
        for (int index = 0; index < 200; index++) {
            store.put(List.of(row), true);
        }
        assertThat(store.find(first)).isEmpty();
    }

    private int productCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class);
    }

    private static String[] row(String productCode, String slug, String categorySlug,
                                String nameZh, String status) {
        return new String[] {productCode, slug, categorySlug, nameZh, status};
    }

    private static MockMultipartFile workbook(List<String[]> rows) throws Exception {
        return workbook(List.of("productCode", "slug", "categorySlug", "nameZh", "status"), rows);
    }

    private static MockMultipartFile workbook(List<String> headers, List<String[]> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Products");
            Row header = sheet.createRow(0);
            headers
                    .forEach(value -> header.createCell(header.getLastCellNum() < 0 ? 0 : header.getLastCellNum())
                            .setCellValue(value));
            int index = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(index++);
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "products.xlsx", XLSX, output.toByteArray());
        }
    }

    private static MockMultipartFile xlsxWithExtraEntries(int entryCount) throws Exception {
        byte[] base = workbook(List.<String[]>of(
                row("YT-ZIP", "zip-product", "fasteners", "压缩包产品", "DRAFT"))).getBytes();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(base))) {
                ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    zip.putNextEntry(new ZipEntry(entry.getName()));
                    input.transferTo(zip);
                    zip.closeEntry();
                }
            }
            for (int index = 0; index < entryCount; index++) {
                zip.putNextEntry(new ZipEntry("synthetic/entry-" + index + ".xml"));
                zip.write("<x/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return new MockMultipartFile("file", "products.xlsx", XLSX, output.toByteArray());
        }
    }

    private static MockMultipartFile xlsxWithLargeExtraEntries(int entryCount, int bytesPerEntry)
            throws Exception {
        byte[] base = workbook(List.<String[]>of(
                row("YT-ZIP", "zip-product", "fasteners", "压缩包产品", "DRAFT"))).getBytes();
        byte[] block = new byte[8192];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(base))) {
                ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    zip.putNextEntry(new ZipEntry(entry.getName()));
                    input.transferTo(zip);
                    zip.closeEntry();
                }
            }
            for (int index = 0; index < entryCount; index++) {
                zip.putNextEntry(new ZipEntry("synthetic/large-entry-" + index + ".xml"));
                int remaining = bytesPerEntry;
                while (remaining > 0) {
                    int size = Math.min(remaining, block.length);
                    zip.write(block, 0, size);
                    remaining -= size;
                }
                zip.closeEntry();
            }
            zip.finish();
            return new MockMultipartFile("file", "products.xlsx", XLSX, output.toByteArray());
        }
    }

    private static MockMultipartFile formulaWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Products");
            Row header = sheet.createRow(0);
            List.of("productCode", "slug", "categorySlug", "nameZh", "status")
                    .forEach(value -> header.createCell(header.getLastCellNum() < 0 ? 0 : header.getLastCellNum())
                            .setCellValue(value));
            Row row = sheet.createRow(1);
            row.createCell(0).setCellFormula("1+1");
            row.createCell(1).setCellValue("formula-product");
            row.createCell(2).setCellValue("fasteners");
            row.createCell(3).setCellValue("公式产品");
            row.createCell(4).setCellValue("DRAFT");
            workbook.write(output);
            return new MockMultipartFile("file", "products.xlsx", XLSX, output.toByteArray());
        }
    }
}
