package com.yongtuo.site.article;

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
class ArticleServiceTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    long category;
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM article");
        jdbc.update("DELETE FROM article_category WHERE slug='synthetic-category'");
        jdbc.update("DELETE FROM url_redirect");
        category = jdbc.queryForObject("SELECT MIN(id) FROM article_category", Long.class);
    }

    @Test void publicCategoryFilterAppliesBeforePagination() throws Exception {
        mvc.perform(post("/api/v1/admin/articles").with(user("admin")).contentType("application/json").content(body("synthetic-filter", "PUBLISHED"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/articles").param("category", "nonexistent")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        String slug = jdbc.queryForObject("SELECT slug FROM article_category WHERE id=?", String.class, category);
        mvc.perform(get("/api/v1/public/articles").param("category", slug)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }
    @Test void visibilityMatrixNeverExposesDraftEnglishOrOfflineContent() throws Exception {
        int sequence = 0;
        for (String mode : new String[]{"ZH_ONLY", "EN_ONLY", "BILINGUAL"}) {
            for (String state : new String[]{"DRAFT", "PUBLISHED", "OFFLINE"}) {
                for (String english : new String[]{"EMPTY", "AI_DRAFT", "CONFIRMED"}) {
                    String slug = "synthetic-" + sequence++;
                    jdbc.update("""
                            INSERT INTO article(category_id,slug,title_zh,title_en,content_zh,content_en,
                                language_mode,english_status,status)
                            VALUES (?,?,'测试中文','Synthetic English','中文正文','Synthetic body',?,?,?)
                            """, category, slug, mode, english, state);
                    for (String language : new String[]{"zh-CN", "en"}) {
                        boolean en = language.equals("en");
                        boolean visible = state.equals("PUBLISHED") && (en
                                ? !mode.equals("ZH_ONLY") && english.equals("CONFIRMED") : !mode.equals("EN_ONLY"));
                        var result = mvc.perform(get("/api/v1/public/articles/" + slug).header("Accept-Language", language))
                                .andExpect(status().is(visible ? 200 : 404));
                        if (visible) result.andExpect(jsonPath("$.data.title").value(en ? "Synthetic English" : "测试中文"))
                                .andExpect(jsonPath("$.data.contentEn").doesNotExist());
                    }
                }
            }
        }
        mvc.perform(get("/api/v1/public/articles").header("Accept-Language", "en"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/api/v1/public/articles").header("Accept-Language", "zh-CN"))
                .andExpect(jsonPath("$.data.total").value(6));
    }

    @Test void categoriesCrudSoftDeleteAndPublicationRedirectsWork() throws Exception {
        assertThat(jdbc.queryForList("SELECT name_zh FROM article_category ORDER BY sort_order", String.class))
                .containsExactly("公司动态", "产品知识", "CNC 加工知识", "行业应用");
        mvc.perform(post("/api/v1/admin/articles").contentType("application/json").content(body("draft", "DRAFT")))
                .andExpect(status().isUnauthorized());
        var created = mvc.perform(post("/api/v1/admin/articles").with(user("admin"))
                        .contentType("application/json").content(body("synthetic-old", "PUBLISHED")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.data.id")).longValue();
        mvc.perform(put("/api/v1/admin/articles/" + id).with(user("admin"))
                        .contentType("application/json").content(body("synthetic-new", "PUBLISHED")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/redirects").param("path", "/en/articles/synthetic-old"))
                .andExpect(jsonPath("$.data.newPath").value("/en/articles/synthetic-new"));
        mvc.perform(post("/api/v1/admin/articles").with(user("admin"))
                        .contentType("application/json").content(body("synthetic-new", "DRAFT")))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/admin/articles/" + id).with(user("admin"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/articles/synthetic-new")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE deleted_at IS NOT NULL", Integer.class)).isEqualTo(1);
    }

    private String body(String slug, String state) {
        return """
                {"categoryId":%d,"slug":"%s","titleZh":"测试文章","titleEn":"Synthetic article",
                "contentZh":"中文正文","contentEn":"Synthetic body","languageMode":"BILINGUAL",
                "englishStatus":"CONFIRMED","status":"%s","isFeatured":false,"sortOrder":0}
                """.formatted(category, slug, state);
    }

    @Test void categoryMaintenanceProtectsReferencesAndEnglishDraftStaysHidden() throws Exception {
        mvc.perform(get("/api/v1/admin/article-categories").with(user("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(4));
        var created = mvc.perform(post("/api/v1/admin/article-categories").with(user("admin"))
                        .contentType("application/json").content("""
                        {"nameZh":"测试分类","nameEn":"Synthetic category","slug":"synthetic-category","sortOrder":10,"status":"ACTIVE"}
                        """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        category = ((Number)JsonPath.read(created,"$.data.id")).longValue();
        mvc.perform(post("/api/v1/admin/articles").with(user("admin")).contentType("application/json")
                .content(body("english-draft", "PUBLISHED").replace("CONFIRMED", "AI_DRAFT")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/articles/english-draft").header("Accept-Language","en"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/admin/article-categories/" + category).with(user("admin")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/article-categories/" + category).with(user("admin"))
                        .contentType("application/json").content("""
                        {"nameZh":"测试分类","nameEn":"Synthetic category","slug":"synthetic-category","sortOrder":10,"status":"INACTIVE"}
                        """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/articles/english-draft")).andExpect(status().isNotFound());
    }
}
