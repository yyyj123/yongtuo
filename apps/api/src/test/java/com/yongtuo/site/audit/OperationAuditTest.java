package com.yongtuo.site.audit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class OperationAuditTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
    @Test void recordsEditsImportsAndContactUpdatesWithoutBodiesOrTokens() throws Exception {
        long category=jdbc.queryForObject("SELECT MIN(id) FROM product_category",Long.class);
        String slug=jdbc.queryForObject("SELECT slug FROM product_category WHERE id=?",String.class,category);
        jdbc.update("INSERT INTO product(category_id,product_code,name_zh,slug,status,english_status) VALUES (?,'SYNTHETIC-AUDIT','测试审计','synthetic-audit','DRAFT','EMPTY')",category);
        long id=jdbc.queryForObject("SELECT id FROM product WHERE slug='synthetic-audit'",Long.class);
        mvc.perform(put("/api/v1/admin/products/"+id).with(user("admin")).contentType("application/json").content("""
                {"categoryId":%d,"productCode":"SYNTHETIC-AUDIT","nameZh":"测试修改","slug":"synthetic-audit","status":"DRAFT",
                "englishStatus":"EMPTY","isFeatured":false,"sortOrder":1,"attributes":[],"variants":[]}
                """.formatted(category))).andExpect(status().isOk());
        String preview=mvc.perform(multipart("/api/v1/admin/products/import/preview").file(workbook(slug)).with(user("admin")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=JsonPath.read(preview,"$.data.importToken");
        mvc.perform(post("/api/v1/admin/products/import/confirm").with(user("admin")).contentType("application/json")
                .content("{\"importToken\":\""+token+"\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/contact").with(user("admin")).contentType("application/json").content("""
                [{"type":"EMAIL","value":"private-audit@example.com","sortOrderZh":0,"sortOrderEn":0,"enabled":true}]
                """)).andExpect(status().isOk());
        var logs=jdbc.queryForList("SELECT * FROM admin_operation_log");
        assertThat(logs).hasSize(4);
        assertThat(logs.toString()).contains("products/{id}","products/import/confirm","contact")
                .doesNotContain(token,"private-audit@example.com","测试修改","password","PK");
        mvc.perform(get("/api/v1/admin/operation-logs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/operation-logs").with(user("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(4));
    }
    private static MockMultipartFile workbook(String category) throws Exception {
        try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var bytes=new java.io.ByteArrayOutputStream()) {
            var sheet=book.createSheet("Products");
            String[][] values={{"productCode","slug","categorySlug","nameZh","status"},{"SYNTHETIC-AUDIT-IMPORT","synthetic-audit-import",category,"测试导入","DRAFT"}};
            for(int i=0;i<values.length;i++) {var row=sheet.createRow(i);for(int j=0;j<values[i].length;j++) row.createCell(j).setCellValue(values[i][j]);}
            book.write(bytes);return new MockMultipartFile("file","synthetic.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes.toByteArray());
        }
    }
}
