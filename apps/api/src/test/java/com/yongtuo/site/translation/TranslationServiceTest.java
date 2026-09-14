package com.yongtuo.site.translation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.yongtuo.site.common.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers @Import(TranslationServiceTest.FakeConfiguration.class)
class TranslationServiceTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
    long id;
    @BeforeEach void seed() {
        jdbc.update("DELETE FROM product");
        long category=jdbc.queryForObject("SELECT MIN(id) FROM product_category",Long.class);
        jdbc.update("INSERT INTO product(category_id,product_code,name_zh,description_zh,slug,status,english_status) VALUES (?,'SYNTHETIC-TRANSLATION','测试名称','测试正文','synthetic-translation','PUBLISHED','EMPTY')",category);
        id=jdbc.queryForObject("SELECT id FROM product WHERE slug='synthetic-translation'",Long.class);
    }
    @Test void disabledProviderReportsFeatureNotConfigured() {
        assertThatThrownBy(()->new DisabledTranslationProvider().translate(new TranslationRequest("PRODUCT",id,Map.of("name","测试"))))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo(50001));
    }
    @Test void draftIsStoredWithoutPublicationOrTechnicalFieldChanges() throws Exception {
        draft(false).andExpect(status().isOk()).andExpect(jsonPath("$.data.englishStatus").value("AI_DRAFT"));
        assertThat(jdbc.queryForObject("SELECT english_status FROM product WHERE id=?",String.class,id)).isEqualTo("AI_DRAFT");
        assertThat(jdbc.queryForObject("SELECT product_code FROM product WHERE id=?",String.class,id)).isEqualTo("SYNTHETIC-TRANSLATION");
        mvc.perform(get("/api/v1/public/products/synthetic-translation").header("Accept-Language","en")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/translation/draft").with(user("admin")).contentType("application/json")
                .content("{\"resourceType\":\"PRODUCT\",\"resourceId\":"+id+",\"fields\":[\"productCode\"],\"replaceConfirmed\":false}"))
                .andExpect(status().isBadRequest());
    }
    @Test void confirmedEnglishRequiresExplicitReplacementAndSeparateConfirmation() throws Exception {
        jdbc.update("UPDATE product SET name_en='Human title',description_en='Human content',english_status='CONFIRMED' WHERE id=?",id);
        draft(false).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT name_en FROM product WHERE id=?",String.class,id)).isEqualTo("Human title");
        draft(true).andExpect(status().isOk());
        String expected="{\"name\":\"Synthetic translated name\",\"description\":\"Synthetic translated description\"}";
        mvc.perform(post("/api/v1/admin/translation/confirm").with(user("admin")).contentType("application/json")
                .content("{\"resourceType\":\"PRODUCT\",\"resourceId\":"+id+",\"expectedFields\":{\"name\":\"stale\"}}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/translation/confirm").with(user("admin")).contentType("application/json")
                .content("{\"resourceType\":\"PRODUCT\",\"resourceId\":"+id+",\"expectedFields\":"+expected+"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT english_status FROM product WHERE id=?",String.class,id)).isEqualTo("CONFIRMED");
    }
    private org.springframework.test.web.servlet.ResultActions draft(boolean replace) throws Exception {
        return mvc.perform(post("/api/v1/admin/translation/draft").with(user("admin")).contentType("application/json")
                .content("{\"resourceType\":\"PRODUCT\",\"resourceId\":"+id+",\"fields\":[\"name\",\"description\"],\"replaceConfirmed\":"+replace+"}"));
    }
    @TestConfiguration static class FakeConfiguration {
        @Bean @Primary TranslationProvider fakeProvider() {
            return request->new TranslationDraft(request.sourceFields().keySet().stream().collect(java.util.stream.Collectors.toMap(k->k,k->"Synthetic translated "+k)));
        }
    }
}
