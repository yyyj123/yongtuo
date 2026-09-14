package com.yongtuo.site.site;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class SiteContentTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @Test void emailUpdatedOnceIsImmediatelySharedEverywhere() throws Exception {
        for(String email:new String[]{"old@example.com","new@example.com"}) {
            mvc.perform(put("/api/v1/admin/contact").with(user("admin")).contentType("application/json").content("""
                    [{"type":"EMAIL","labelZh":"邮箱","labelEn":"Email","value":"%s",
                    "linkUrl":"mailto:%s","sortOrderZh":0,"sortOrderEn":0,"enabled":true}]
                    """.formatted(email,email))).andExpect(status().isOk());
            mvc.perform(get("/api/v1/public/contact")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].value").value(email));
            for(String route:new String[]{"home","site"}) mvc.perform(get("/api/v1/public/"+route))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.contact[0].value").value(email));
        }
    }
    @Test void fixedStructureAndFeaturedCountsAreEnforced() throws Exception {
        for(String kind:new String[]{"products","cases","certificates","articles"}) {
            String ids=kind.equals("products")?"[1,2,3,4,5,6,7,8,9]":kind.equals("articles")?"[1,2]":"[1,2,3,4,5]";
            mvc.perform(put("/api/v1/admin/home/featured-"+kind).with(user("admin")).contentType("application/json").content(ids))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/v1/admin/home/arbitrary-block").with(user("admin"))
                .contentType("application/json").content("{\"enabled\":true,\"englishStatus\":\"EMPTY\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/site").with(user("admin")).contentType("application/json")
                .content("{\"arbitrary_key\":{\"valueZh\":\"test\",\"englishStatus\":\"EMPTY\"}}"))
                .andExpect(status().isBadRequest());
    }
    @Test void configuredSectionsAreLocalizedAndDraftEnglishIsHidden() throws Exception {
        mvc.perform(put("/api/v1/admin/home/hero").with(user("admin")).contentType("application/json").content("""
                {"titleZh":"测试标题","titleEn":"Synthetic title","contentZh":"中文正文","contentEn":"Synthetic draft",
                "englishStatus":"AI_DRAFT","enabled":true}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/home").header("Accept-Language","zh-CN"))
                .andExpect(jsonPath("$.data.hero.title").value("测试标题"));
        String english=mvc.perform(get("/api/v1/public/home").header("Accept-Language","en"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(english).doesNotContain("Synthetic draft","测试标题");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM home_section",Integer.class)).isEqualTo(5);
    }
    @Test void featuredContentUsesLiveReferencesAndHidesUnpublishedSources() throws Exception {
        long category=jdbc.queryForObject("SELECT MIN(id) FROM product_category",Long.class);
        jdbc.update("INSERT INTO product(category_id,product_code,name_zh,slug,status,english_status) VALUES (?,'SYNTHETIC-HOME','测试首页产品','synthetic-home','PUBLISHED','EMPTY')",category);
        long id=jdbc.queryForObject("SELECT id FROM product WHERE slug='synthetic-home'",Long.class);
        mvc.perform(put("/api/v1/admin/home/featured-products").with(user("admin")).contentType("application/json").content("["+id+"]"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE product SET name_zh='测试新名称' WHERE id=?",id);
        mvc.perform(get("/api/v1/public/home").header("Accept-Language","zh-CN"))
                .andExpect(jsonPath("$.data.featuredProducts[0].title").value("测试新名称"));
        mvc.perform(get("/api/v1/public/home").header("Accept-Language","en"))
                .andExpect(jsonPath("$.data.featuredProducts.length()").value(0));
        jdbc.update("UPDATE product SET status='OFFLINE' WHERE id=?",id);
        mvc.perform(get("/api/v1/public/home")).andExpect(jsonPath("$.data.featuredProducts.length()").value(0));
    }
    @Test void nestedInputsAreValidatedAtTheWriteBoundary() throws Exception {
        mvc.perform(put("/api/v1/admin/contact").with(user("admin")).contentType("application/json").content("[{\"value\":\"missing-type\"}]"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/site").with(user("admin")).contentType("application/json").content("{\"brand\":{\"valueZh\":\"missing-status\"}}"))
                .andExpect(status().isBadRequest());
    }
}
