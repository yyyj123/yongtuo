package com.yongtuo.site.casestudy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class CaseStudyServiceTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    long category, otherCategory, product, otherProduct;
    @BeforeEach void seed() {
        jdbc.update("DELETE FROM case_study");
        jdbc.update("DELETE FROM product");
        var categories = jdbc.queryForList("SELECT id FROM product_category WHERE status='ACTIVE' ORDER BY id LIMIT 2",Long.class);
        category=categories.get(0); otherCategory=categories.get(1);
        for (int i=1;i<=2;i++) jdbc.update("""
                INSERT INTO product(category_id,product_code,slug,name_zh,name_en,status,english_status)
                VALUES (?,?,?,'测试产品','Synthetic product','PUBLISHED','CONFIRMED')
                """,category,"SYNTHETIC-"+i,"synthetic-product-"+i);
        product=jdbc.queryForObject("SELECT id FROM product WHERE product_code='SYNTHETIC-1'",Long.class);
        otherProduct=jdbc.queryForObject("SELECT id FROM product WHERE product_code='SYNTHETIC-2'",Long.class);
    }
    @Test void linksMultipleProductsAndCategoriesAndReverseLookupHonorsVisibility() throws Exception {
        String created=mvc.perform(post("/api/v1/admin/cases").with(user("admin")).contentType("application/json")
                        .content(body("synthetic-case",false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productIds.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id=((Number)JsonPath.read(created,"$.data.base.id")).longValue();
        mvc.perform(get("/api/v1/public/cases/synthetic-case").header("Accept-Language","en"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.products.length()").value(2))
                .andExpect(jsonPath("$.data.categories.length()").value(2));
        mvc.perform(get("/api/v1/public/products/synthetic-product-1").header("Accept-Language","en"))
                .andExpect(jsonPath("$.data.relatedCases[0].slug").value("synthetic-case"));
        mvc.perform(put("/api/v1/admin/cases/"+id).with(user("admin")).contentType("application/json")
                .content(body("synthetic-case",false).replace("CONFIRMED","AI_DRAFT")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/products/synthetic-product-1").header("Accept-Language","en"))
                .andExpect(jsonPath("$.data.relatedCases.length()").value(0));
        mvc.perform(get("/api/v1/public/products/synthetic-product-1").header("Accept-Language","zh-CN"))
                .andExpect(jsonPath("$.data.relatedCases.length()").value(1));
        mvc.perform(delete("/api/v1/admin/cases/"+id).with(user("admin"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/cases/synthetic-case")).andExpect(status().isNotFound());
    }
    @Test void featuredLimitAndInvalidRelationsAreTransactional() throws Exception {
        for(int i=0;i<4;i++) mvc.perform(post("/api/v1/admin/cases").with(user("admin"))
                .contentType("application/json").content(body("synthetic-featured-"+i,true))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/cases").with(user("admin"))
                .contentType("application/json").content(body("synthetic-overflow",true))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/cases").with(user("admin"))
                .contentType("application/json").content(body("synthetic-invalid",false)
                        .replace("\"productIds\":["+product+","+otherProduct+"]","\"productIds\":[999999999]")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM case_study",Integer.class)).isEqualTo(4);
    }
    private String body(String slug, boolean featured) {
        return """
                {"slug":"%s","titleZh":"测试案例","titleEn":"Synthetic case","contentZh":"测试正文",
                "contentEn":"Synthetic body","languageMode":"BILINGUAL","englishStatus":"CONFIRMED",
                "status":"PUBLISHED","isFeatured":%s,"sortOrder":0,"productIds":[%d,%d],"categoryIds":[%d,%d],
                "imageUrls":["https://example.com/synthetic.jpg"]}
                """.formatted(slug,featured,product,otherProduct,category,otherCategory);
    }
}
