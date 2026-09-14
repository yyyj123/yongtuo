package com.yongtuo.site.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class ManagedRichTextTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
    @Test void managedHtmlIsCleanInStorageAndPublicResponses() throws Exception {
        mvc.perform(put("/api/v1/admin/home/hero").with(user("admin")).contentType("application/json").content("""
                {"titleZh":"测试安全标题","contentZh":"<p onmouseover='evil()'>Safe</p><script>alert(1)</script>",
                "englishStatus":"EMPTY","enabled":true}
                """)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT content_zh FROM home_section WHERE section_code='HERO'",String.class)).isEqualTo("<p>Safe</p>");
        mvc.perform(get("/api/v1/public/home").header("Accept-Language","zh-CN"))
                .andExpect(jsonPath("$.data.hero.content").value("<p>Safe</p>"));
        long category=jdbc.queryForObject("SELECT MIN(id) FROM article_category",Long.class);
        mvc.perform(post("/api/v1/admin/articles").with(user("admin")).contentType("application/json").content("""
                {"categoryId":%d,"slug":"synthetic-xss","titleZh":"测试文章","contentZh":"<p>Safe</p><img src='x' onerror='evil()'>",
                "languageMode":"ZH_ONLY","englishStatus":"EMPTY","status":"PUBLISHED","isFeatured":false,"sortOrder":0}
                """.formatted(category))).andExpect(status().isOk());
        String stored=jdbc.queryForObject("SELECT content_zh FROM article WHERE slug='synthetic-xss'",String.class);
        assertThat(stored).contains("<p>Safe</p>").doesNotContain("onerror","evil");
    }
}
